import com.sap.gateway.ip.core.customdev.util.Message;
import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def Message processXmlInput(Message message) {

  def body = message.getBody(java.io.Reader)
  def xml = new XmlParser().parse(body)

  //find tags with null value
  def results = xml.
  '**'.findAll {
    !it.value()
  }

  results.each {
    def newNode = new Node(null, it.name(), 'xsi.nil')
    // replace node    
    it.replaceNode(newNode)
  }
  // write to body
  StringWriter stringWriter = new StringWriter()
  XmlNodePrinter nodePrinter = new XmlNodePrinter(new PrintWriter(stringWriter))
  nodePrinter.setPreserveWhitespace(true)
  nodePrinter.print xml

  message.setBody(stringWriter.toString())
  return message;

}

def Message enrichJsonOutput(Message message) {
  //Body 
  def body = message.getBody(java.io.Reader);

  JsonSlurper slurper = new JsonSlurper()
  Map parsedJson = slurper.parse(body)

  parsedJson.messageRequests.each {
      
// doImplicitAddressMatching      
    it.body.doImplicitAddressMatching = Boolean.parseBoolean(it.body.doImplicitAddressMatching)
    
//addressUsage/isDefault
    it.body.addresses.each {
      it.addressUsage.each {
        if (it.isDefault) {
          it.isDefault = Boolean.parseBoolean(it.isDefault)
        }
      }
//address/commlines/isUsageDenied      
      it.address.telephone.each {
        if (it.isUsageDenied) {
            it.isUsageDenied = Boolean.parseBoolean(it.isUsageDenied)
        }
        if (it.isMobilePhoneNumber) {
            it.isMobilePhoneNumber = Boolean.parseBoolean(it.isMobilePhoneNumber)
        }
      }
     it.address.email.each {
        if (it.isUsageDenied) {
            it.isUsageDenied = Boolean.parseBoolean(it.isUsageDenied)
        }
      }
     it.address.web.each {
        if (it.isUsageDenied) {
            it.isUsageDenied = Boolean.parseBoolean(it.isUsageDenied)
        }
      }   
     it.address.facsimile.each {
        if (it.isUsageDenied) {
            it.isUsageDenied = Boolean.parseBoolean(it.isUsageDenied)
        }
      }       
      
     it.address.postalAddress.each {
        if (it.isPostOfficeBoxWithoutId) {
            it.isPostOfficeBoxWithoutId = Boolean.parseBoolean(it.isPostOfficeBoxWithoutId)
        }
     }
    }
     
//businessPartnerIndustrySector/isDefault
    it.body.businessPartnerIndustrySector.each {
      if (it.isDefault) {
        it.isDefault = Boolean.parseBoolean(it.isDefault)
      }
    }

//common/isBlocked, isDeleted, isReleased
    it.body.common.each {
        if (it.isBlocked) {
          it.isBlocked = Boolean.parseBoolean(it.isBlocked)
        }
        if (it.isDeleted) {
          it.isDeleted = Boolean.parseBoolean(it.isDeleted)
        }
        if (it.isReleased) {
          it.isReleased = Boolean.parseBoolean(it.isReleased)
        }
        if (it.isNaturalPerson) {
          it.isNaturalPerson = Boolean.parseBoolean(it.isNaturalPerson)
        }
    }

// if customer node exists
  if (it.body.customer) {
//customer/salesArrangements/deliveryTerms/isCompleteDeliveryRequested
    it.body.customer.salesArrangements.each {
        if (it.deliveryTerms.isCompleteDeliveryRequested) {
          it.deliveryTerms.isCompleteDeliveryRequested = Boolean.parseBoolean(it.deliveryTerms.isCompleteDeliveryRequested)
        }
        it.partnerFunctionRelationships.each {
            if (it.isDefault){
                it.isDefault = Boolean.parseBoolean(it.isDefault)
            }
        }
    }
//blockingReasons/isSalesSupportBlocked
    if (it.body.customer.blockingReasons.isSalesSupportBlocked) {
      it.body.customer.blockingReasons.isSalesSupportBlocked = Boolean.parseBoolean(it.body.customer.blockingReasons.isSalesSupportBlocked)
    }

//customer/salesArrangements/isSalesSupportBlocked
    it.body.customer.salesArrangements.each {
      if (it.isSalesSupportBlocked) {
        it.isSalesSupportBlocked = Boolean.parseBoolean(it.isSalesSupportBlocked)
      }
    }

  }

  }

  def respbody = JsonOutput.toJson(parsedJson)
  // handle xml nil 
  def body2 = respbody.replace('"xsi.nil"','null')
  message.setBody(body2)
  
  return message;
}