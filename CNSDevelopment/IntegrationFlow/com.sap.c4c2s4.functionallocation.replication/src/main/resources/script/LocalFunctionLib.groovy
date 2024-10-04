import com.sap.it.api.mapping.*;


def String SystemID(String systemID, MappingContext context){
	return context.getProperty(systemID);
}

def String InstallationPointID(String funLocNo, MappingContext context){
    context.setProperty("P_FunLocNum",funLocNo)
	return context.getProperty("P_FunctionalLocationID")
}


def String determineStatus(String FuncnlLocIsMarkedForDeletion,String FuncnlLocIsDeleted,String FunctionalLocationIsActive,String FuncnlLocIsDeactivated,MappingContext context){
      if(FuncnlLocIsMarkedForDeletion == "true" || FuncnlLocIsDeleted == "true")
         return "4";
       else if (FuncnlLocIsDeactivated == "true") 
         return "3";  
       else if (FunctionalLocationIsActive == "true")
         return "2";
       else
         return "1";
}   

def String getFunLocURI(String arg1,MappingContext context){
    //PUT FunctionalLocation(FunctionalLocation='10180054') HTTP/1.1   
    def uri = "FunctionalLocation(FunctionalLocation='" + context.getProperty("P_FunctionalLocationID") + "')"
	return uri 
}

def String getFunLocBatchURI(String arg1,MappingContext context){
    //PUT FunctionalLocation(FunctionalLocation='10180054') HTTP/1.1   
    def uri = "FunctionalLocation(FunctionalLocation='" + context.getProperty("P_FunctionalLocationID") + "')"
	return uri 
}

def String getFunctionalLocationURI(String arg1,MappingContext context){
    //PUT FunctionalLocation(FunctionalLocation='10180054') HTTP/1.1   
    def uri = "FunctionalLocation(FunctionalLocation='" + context.getProperty("P_PartnerFunLocNum") + "')"
	return uri 
}

def String getFunLocPartnerURI(String arg1,MappingContext context){
    //PUT FunctionalLocation(FunctionalLocation='10180054') HTTP/1.1   
    def uri = "FunctionalLocation(FunctionalLocation='" + context.getProperty("P_PartnerFunLocNum") + "')" + "/to_Partner"
	return uri 
}

def String getSenderSequenceNo(String dummy,MappingContext context){
    //The etag from S/4 is of format W/"'SADL-020201207134153C~20201207134153'" and we extract the timestamp 20201207134153 using substring
    def _etag = context.getHeader('etag')
    return _etag.substring(26,40)
}

def String getEtag(String sequenceno,MappingContext context){
   //return "W/\"'SADL-0" +  sequenceno + "C~"+sequenceno + "'\""
   // return context.getHeader('etag')
   return context.getProperty("P_etag_value")
}

def String getFunLocLongTextURI(String arg1,MappingContext context){
    //PATCH FunctionalLocationLongText(FunctionalLocation='10180054') HTTP/1.1
    def uri = "FunctionalLocationLongText(FunctionalLocation='" + context.getProperty('P_FunLocNum') + "')"
	return uri 
}

def String getFunLocCategory(String arg1,MappingContext context){
    context.getProperty("P_FunLocCategory")
}

def String getStructureInd(String arg1,MappingContext context){
    context.getProperty("P_Structure_Indicator")
}

def String getTextTypeCode(String arg1, MappingContext context){
    context.getProperty("P_TextTypeCode")
}

def String generateMessageHeaderId(String arg1, MappingContext context){
	String messageHeaderId =  UUID.randomUUID().toString()
	context.setProperty("P_MessageHeaderId", messageHeaderId)
	return messageHeaderId
}

def String generateGuid(String arg1){
	return UUID.randomUUID()
}

def String getC4CDisplayID(String arg1,MappingContext context){
    return context.getProperty("P_FunctionalLocationID")
}

def String getLanguage(String arg1,MappingContext context){
    return context.getProperty("P_Language")
}










