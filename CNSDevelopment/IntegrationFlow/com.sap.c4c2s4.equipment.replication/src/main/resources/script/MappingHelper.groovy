import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.xml.MarkupBuilder;
import com.sap.it.api.mapping.*;
import groovy.json.*;

/**
 * If a partner function in C4C exists and not in S/4 then it is considered as a create (POST) case
 * If a partner function exists in both C4C and S/4 but the partner no is different, then it is considered as an update (PATCH) case
 * MarkupBuilder is used to construct the batch output. 
 * Each partner function to be added or updated are added to batch as a separate batchChangeSetPart under batchParts/batchChangeSet
 * URI for PATCH Method is of format:
 *       PATCH EquipmentPartner(Equipment='10006851',PartnerFunction='RE',EquipmentPartnerObjectNmbr='2') HTTP/1.1
 * URI for POST Method is of format:
 *       POST EquipmentPartner HTTP/1.1
 */

def String getEquipmentWarrantyURI(String input, MappingContext context){
  def uri = "EquipmentWarranty(Equipment='" + context.getProperty('P_S4_EquipmentNum') + "',WarrantyType='"+context.getProperty('P_WarrantyType')+"')"
  return uri
}

def Message processPartnerFunction(Message message) {

  def body = message.getBody(java.io.Reader)
  def s4Party = new XmlSlurper().parse(body)

  def map = message.getProperties()
  def equipmentNo = map.get("P_S4_EquipmentNum")
  //def c4cParty = new XmlSlurper().parseText(map.get("P_C4C_InvolvedParties"))
  def c4cParty = new XmlSlurper().parse(map.get("P_C4C_InvolvedParties"))

  def patchlist = []
  def postlist = []

  for (party in c4cParty.Party) {

    def partnerRole = party.PartnerFunction.toString()
    def partnerNo = party.Partner.toString()
    def existingParty = false

    for (sparty in s4Party.EquipmentType.to_Partner.EquipmentPartnerType) {

      def s4PartnerRole = sparty.PartnerFunction.toString()
      def s4PartnerNo = sparty.Partner.toString()

      if (s4PartnerRole.equals(partnerRole) && s4PartnerNo.equals(partnerNo)) existingParty = true

      if (s4PartnerRole.equals(partnerRole) && !s4PartnerNo.equals(partnerNo)) {
        patchlist.add(party.PartnerFunction + sparty.EquipmentPartnerObjectNmbr + party.Partner)
        existingParty = true
      }
    }

    if (!existingParty) postlist.add(party.PartnerFunction + party.Partner)
  }

  def xmlWriter = new StringWriter()
  def xmlMarkup = new MarkupBuilder(xmlWriter)
  xmlMarkup.batchParts {
    batchChangeSet {
      for (e in patchlist) {
        batchChangeSetPart {
          method('PATCH')
          uri("EquipmentPartner(Equipment='" + equipmentNo + "',PartnerFunction='" + e[0] + "',EquipmentPartnerObjectNmbr='" + e[1] + "')")
          EquipmentPartner {
            EquipmentPartnerType {
              Partner(e[2])
            }
          }
        }

      }
      for (e in postlist) {
        batchChangeSetPart {
          method('POST')
          uri('EquipmentPartner')
          EquipmentPartner {
            EquipmentPartnerType {
              Equipment(equipmentNo)
              PartnerFunction(e[0])
              Partner(e[1])
            }
          }
        }
      }
    }
  }
  String result = xmlWriter.toString()
  message.setBody(result)
  return message
}

/**
 * Creates Equipment long text batch payload
 * Format : PATCH EquipmentLongText(Equipment='10006844') HTTP/1.1
 * 
 */

def Message processTextCollection(Message message) {

  def body = message.getBody(java.io.Reader)
  def content = new XmlSlurper().parse(body)

  def textContent = content.Text.ContentText.toString()

  def map = message.getProperties()
  def equipmentNo = map.get("P_S4_EquipmentNum")

  def xmlWriter = new StringWriter()
  def xmlMarkup = new MarkupBuilder(xmlWriter)

  xmlMarkup.batchParts {
    batchChangeSet {
      if (textContent.size() > 0) batchChangeSetPart {
        method('PATCH')
        uri("EquipmentLongText(Equipment='" + equipmentNo + "')")
        EquipmentLongText {
          EquipmentLongTextType {
            EquipmentLongText(textContent)
          }
        }
      }
    }
  }

  String result = xmlWriter.toString()
  message.setBody(result)
  return message

}

/**
 * Equipment Hierarchy Processing 
 * If C4C ReceiverUpperInstallationPointID exists and S/4 SuperordinateEquipment is initial - Do an Installation
 * If S/4 SuperordinateEquipment exists and C4C ReceiverUpperInstallationPointID is initial - Do a Dismantle
 * If both exists and the values are not equal, first Dismantle (existing SuperordinateEquipment) and then install (C4C ReceiverUpperInstallationPointID)
 */

def Message processHierachyRelationship(Message message) {

  def map = message.getProperties()
  def c4c_parentequipment = map.get("P_C4CParentEquipment")
  def s4_parentequipment = map.get("P_SuperordinateEquipment")
  def s4_equipmentNo = map.get("P_S4_EquipmentNum")
  def c4c_functionalLocation = map.get("P_C4CFunctionalLocation");
  def s4_functionalLocation = map.get("P_FunctionalLocation")
  def etag = map.get("P_batch_etag")

  def equipInstallationPositionNmbr = "&EquipInstallationPositionNmbr='" + "0000" + "'"

  def xmlWriter = new StringWriter()
  def xmlMarkup = new MarkupBuilder(xmlWriter)

  message.setProperty("c4c_parentequipment.size", c4c_parentequipment.size())
  message.setProperty("s4_parentequipment.size", s4_parentequipment.size())
  message.setProperty("c4c_functionalLocation.size", c4c_functionalLocation.size())
  message.setProperty("s4_functionalLocation.size", s4_functionalLocation.size())

  xmlMarkup.batchParts {
    if (c4c_parentequipment.size() > 0) {
      //update
      if (!c4c_parentequipment.equalsIgnoreCase(s4_parentequipment) &&
        (s4_parentequipment.size() > 0)) {
        batchChangeSet {
          batchChangeSetPart {
            method('POST')
            uri("DismantleEquipment?Equipment='" + s4_equipmentNo + "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27")
            headers {
              header {
                headerName("If-Match")
                headerValue(etag)
              }
            }
          }
        }

        batchChangeSet {
          batchChangeSetPart {
            method('POST')
            uri("InstallEquipment?Equipment='" +
              s4_equipmentNo +
              "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27&SuperordinateEquipment='" +
              c4c_parentequipment +
              "'" +
              equipInstallationPositionNmbr)
            headers {
              header {
                headerName("If-Match")
                headerValue("*")
              }
            }
          }
        }
      } //create
      else if (s4_parentequipment.size() == 0) {
        batchChangeSet {
          batchChangeSetPart {
            method('POST')
            uri("InstallEquipment?Equipment='" +
              s4_equipmentNo +
              "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27&SuperordinateEquipment='" +
              c4c_parentequipment +
              "'" +
              equipInstallationPositionNmbr)
            headers {
              header {
                headerName("If-Match")
                headerValue(etag)
              }
            }
          }
        }

      }
    } //delete
    else if (c4c_parentequipment.size() == 0 && s4_parentequipment.size() > 0) {
      batchChangeSet {
        batchChangeSetPart {
          method('POST')
          uri("DismantleEquipment?Equipment='" + s4_equipmentNo + "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27")
          headers {
            header {
              headerName("If-Match")
              headerValue(etag)
            }
          }
        }
      }
    }
    //done

    if (c4c_functionalLocation.size() > 0) {
      //update
      if (!c4c_functionalLocation.equalsIgnoreCase(s4_functionalLocation) &&
        (s4_functionalLocation.size() > 0)) {
        batchChangeSet {
          batchChangeSetPart {
            method('POST')
            uri("DismantleEquipment?Equipment='" + s4_equipmentNo + "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27")
            headers {
              header {
                headerName("If-Match")
                headerValue(etag)
              }
            }
          }
        }

        batchChangeSet {
          batchChangeSetPart {
            method('POST')
            uri("InstallEquipment?Equipment='" +
              s4_equipmentNo +
              "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27&FunctionalLocation='" +
              c4c_functionalLocation +
              "'" +
              equipInstallationPositionNmbr)
            headers {
              header {
                headerName("If-Match")
                headerValue("*")
              }
            }
          }
        }
      } //create
      else if (s4_functionalLocation.size() == 0) {
        batchChangeSet {
          batchChangeSetPart {
            method('POST')
            uri("InstallEquipment?Equipment='" +
              s4_equipmentNo +
              "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27&FunctionalLocation='" +
              c4c_functionalLocation +
              "'" +
              equipInstallationPositionNmbr)
            headers {
              header {
                headerName("If-Match")
                headerValue(etag)
              }
            }
          }
        }

      }
    } else if (c4c_functionalLocation.size() == 0 && s4_functionalLocation.size() > 0) {
      batchChangeSet {
        batchChangeSetPart {
          method('POST')
          uri("DismantleEquipment?Equipment='" + s4_equipmentNo + "'&ValidityEndDate=datetime%279999-12-31T00%3A00%3A00%27")
          headers {
            header {
              headerName("If-Match")
              headerValue(etag)
            }
          }
        }
      }
    }
  }
  String result = xmlWriter.toString()
  message.setBody(result)
  return message

}

/**
 * Parse the ODATA batch response to extract the message
 * Note the batch response is XML inside XML
 * batchPartResponse is the outer XML root node and body is the inner XML root node
 * Message text is available inside the error/message/text of inner XML
 */

def Message throwBatchProcessingException(Message message) {

  def body = message.getBody(java.io.Reader)
  def errorBody = null

  try {
    def batchPartResponse = new XmlSlurper().parse(body)
    errorBody = new XmlSlurper().parseText(batchPartResponse.batchChangeSetResponse.batchChangeSetPartResponse.body.text())
  } catch (Exception exception) {
    throw new Exception('Batch Processing Exception' + exception)
  }

  if (errorBody != null) throw new Exception(errorBody.message.text())

  return message
}

/**
 * For ODATA post error get the error message text from the ODATA response body and populate it in MPL
 * For Other exceptions return as is
 */

def Message throwODATACreateException(Message message) {

  def body = message.getBody(java.io.Reader)
  def map = message.getProperties();
  def ex = map.get("CamelExceptionCaught")
  def errorBody = null

  if (ex != null) {

    if (ex.getClass().getCanonicalName().equals("com.sap.gateway.core.ip.component.odata.exception.OsciException")) {

      try {
        errorBody = new XmlSlurper().parse(body)
      } catch (Exception exception) {
        throw new Exception(exception)
      }

      if (errorBody.message != null) throw new Exception(errorBody.message.text())
    } else throw new Exception(ex)
  }

  return message
}

def String SystemID(String systemID, MappingContext context) {
  return context.getProperty(systemID)
}

def String getMessageID(String arg1) {
  return UUID.randomUUID()
}

def String generateMessageHeaderId(String arg1, MappingContext context) {
  String messageHeaderId = UUID.randomUUID().toString()
  context.setProperty("P_MessageHeaderId", messageHeaderId)
  return messageHeaderId
}

def String determineStatus(String EquipmentIsMarkedForDeletion, String EquipmentIsAtCustomer, String EquipmentIsAvailable, String EquipmentIsInWarehouse, String EquipmentIsAssignedToDelivery, String EquipmentIsInstalled, String EquipIsAllocToSuperiorEquip, String EquipmentIsInactive, MappingContext context) {

  if (EquipmentIsMarkedForDeletion == "true")
    return "4"

  else if (EquipmentIsAtCustomer == "true" || EquipmentIsAvailable == "true" || EquipmentIsInWarehouse == "true" || EquipmentIsAssignedToDelivery == "true" || EquipmentIsInstalled == "true" || EquipIsAllocToSuperiorEquip == "true" || EquipmentIsInactive == "false")
    return "2"

  else
    return "1"
}

def String getEquipmentURI(String arg1, MappingContext context) {
  //PUT Equipment(Equipment='10180054',ValidityEndDate=datetime'9999-12-31T00%3A00%3A00') HTTP/1.1
  def uri = "Equipment(Equipment='" + context.getProperty('P_S4_EquipmentNum') + "',ValidityEndDate=datetime'9999-12-31T00%3A00%3A00')"
  return uri
}

def String checkIfInstallationRequired(String ID, String Type, MappingContext context) {

  def receiverUpperInstallationPointID = context.getProperty("P_ReceiverUpperInstallationPointID")
  def functionalLocation = context.getProperty("P_FunctionalLocation")
  def superordinateEquipment = context.getProperty("P_SuperordinateEquipment")

  if (receiverUpperInstallationPointID.isEmpty() && functionalLocation.size().isEmpty() && superordinateEquipment.size().isEmpty()) {
    context.setProperty("P_InstalltionRequired", 'false')
    return ""
  }

  if (Type.equalsIgnoreCase("2")) {
    if (receiverUpperInstallationPointID.equalsIgnoreCase(superordinateEquipment)) {
      context.setProperty("P_InstalltionRequired", 'false')
      return ""
    }
  }

  if (Type.equalsIgnoreCase("6")) {
    if (receiverUpperInstallationPointID.equalsIgnoreCase(functionalLocation)) {
      context.setProperty("P_InstalltionRequired", 'false')
      return ""
    }
  }

  context.setProperty("P_InstalltionRequired", 'true')
  return ""
}

def String getSenderSequenceNo(String dummy, MappingContext context) {
  //The etag from S/4 is of format W/"'SADL-020201207134153C~20201207134153'" and we extract the timestamp 20201207134153 using substring
  def _etag = context.getHeader('etag')
  return _etag.substring(26, 40)
}

def String getEtag(String sequenceno, MappingContext context) {
  //return "W/\"'SADL-0" +  sequenceno + "C~"+sequenceno + "'\""
  return context.getHeader('etag')
}

def String getEquipmentLongTextURI(String arg1, MappingContext context) {
  //PATCH EquipmentLongText(Equipment='10180054') HTTP/1.1
  def uri = "EquipmentLongText(Equipment='" + context.getProperty('P_S4_EquipmentNum') + "')"
  return uri
}

def String s4EquipmentNo(String arg1, MappingContext context) {
  return context.getProperty('P_S4_EquipmentNum')
}

def String cnsEquipmentNo(String arg1, MappingContext context) {
  return context.getProperty('P_EquipmentNum')
}

def Message getInstallationPointID(Message message) {
  //Get message and parse to json
  def json = message.getBody(java.io.Reader);
  def data = new JsonSlurper().parse(json);

  message.setProperty("P_SourcePayload", JsonOutput.toJson(data));

  //messageHeader
  message.setHeader("SAP_ApplicationID", data.messageHeader.id);

  //get fields of the payload 
  message.setProperty("P_MessageID", data.messageHeader.id);
  message.setProperty("P_SenderParty", data.messageHeader.senderCommunicationSystemDisplayId);
  message.setProperty("P_ReceiverParty", data.messageHeader.receiverCommunicationSystemDisplayId);

  if (data.messageRequests[0].body.receiverParentRegisteredProductDisplayId || data.messageRequests[0].body.receiverParentFunctionalLocationDisplayId) {
    message.setProperty("P_InstallationRequired", 'true');

  } else {
    message.setProperty("P_InstallationRequired", 'false');
  }
  message.setProperty("P_EquipmentNum", data.messageRequests[0].body.displayId);
  message.setProperty("P_LongText", data.messageRequests[0].body.customerInformation ? data.messageRequests[0].body.customerInformation.content : '');
  message.setProperty("P_Warranty", data.messageRequests[0].body.warranty ? data.messageRequests[0].body.warranty.receiverDisplayId : '');

  message.setProperty("P_S4_EquipmentNum", data.messageRequests[0].body.receiverDisplayId ? data.messageRequests[0].body.receiverDisplayId : '');
  message.setProperty("P_C4CParentEquipment", data.messageRequests[0].body.receiverParentRegisteredProductDisplayId ? data.messageRequests[0].body.receiverParentRegisteredProductDisplayId.toString() : '');
  message.setProperty("P_C4CFunctionalLocation", data.messageRequests[0].body.receiverParentFunctionalLocationDisplayId ? data.messageRequests[0].body.receiverParentFunctionalLocationDisplayId.toString() : '');
  //ids for confirmation message
  message.setProperty("P_messageHeaderId", java.util.UUID.randomUUID())
  message.setProperty("P_messageRequestHeaderId", java.util.UUID.randomUUID())
  return message;
}

//Set Custom Monitoring Header
def Message setCustomConfirmationMessageHeader(Message message) {

  def messageLog = messageLogFactory.getMessageLog(message);
  if (messageLog != null) {

    def messageHeaderId = message.getProperties().get("P_messageHeaderId")
    if (messageHeaderId != null) {
      messageLog.addCustomHeaderProperty("Confirmation Message ID", messageHeaderId.toString())
    }
  }
  return message;
}

def Message retrieveSourcePayload(Message message) {
  //Body
  def body = message.getBody(java.io.Reader)
  def root = new XmlSlurper().parse(body);

  message.setProperty("P_FunctionalLocation", root.EquipmentType.FunctionalLocation ? root.EquipmentType.FunctionalLocation.toString() : '');
  message.setProperty("P_SuperordinateEquipment", root.EquipmentType.SuperordinateEquipment ? root.EquipmentType.SuperordinateEquipment.toString() : '');
  if (root.EquipmentType.FunctionalLocation.toString() || root.EquipmentType.SuperordinateEquipment.toString())
    message.setProperty("P_InstallationRequired", 'true');
  //Properties
  def properties = message.getProperties();
  def value = properties.get("P_SourcePayload");
  message.setBody(value.toString());
  return message;
}

def String getWarrantyType(String inarg, MappingContext context){
	return context.getProperty("P_WarrantyType");
}

def Message setReceiverEquipmentNo(Message message) {

  def json = message.getBody(java.io.Reader);
  def dataStoreEntry = new JsonSlurper().parse(json);
  message.setProperty("P_S4_EquipmentNum", dataStoreEntry.ReceiverInstallationPointID)
  return message
}