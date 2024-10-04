import com.sap.it.api.mapping.*;


def String generateGuid(String arg1){
	return UUID.randomUUID()
}

def String generateMessageHeaderId(String arg1, MappingContext context){
  	String messageHeaderId =  UUID.randomUUID().toString()
  	context.setProperty("P_MessageHeaderId", messageHeaderId)
  	return messageHeaderId
  }

def String getBusinessSystemDisplayId(String arg1, MappingContext context){
    return context.getProperty(arg1)
}

def String getStatus(String EquipmentIsInactive) {

  if (EquipmentIsInactive.equalsIgnoreCase("true"))
    return "INACTIVE"
  else
    return "ACTIVE"
}

def String getLanguage(String arg1, MappingContext context){
    return context.getProperty("P_Language")
}