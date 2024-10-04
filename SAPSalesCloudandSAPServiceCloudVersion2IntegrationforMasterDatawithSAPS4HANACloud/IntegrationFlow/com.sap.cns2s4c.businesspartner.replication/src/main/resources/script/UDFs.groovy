import com.sap.it.api.mapping.*;

def String dateToString17(String arg1){
    
   def str = arg1.replaceAll("[-:.TZ]","")
   arg1 = str.substring(0,17)
   return arg1 
}

def String getIndustryClassificationSystemCode(String propertyName, MappingContext context) {
    
    def propertyValue = context.getProperty("P_IndustryClassificationSystemCode")
    return propertyValue
}

def String setSOAPMessageID(String id,  MappingContext context){
    context.setHeader("SapMessageIdEx", id)
	return id 
}