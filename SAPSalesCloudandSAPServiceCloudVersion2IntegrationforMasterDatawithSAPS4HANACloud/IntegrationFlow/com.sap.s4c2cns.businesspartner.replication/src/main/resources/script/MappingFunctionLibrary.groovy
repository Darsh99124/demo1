import com.sap.it.api.mapping.*;

def String getMessageId(String p1, MappingContext context){
    String messageId = context.getHeader("SapMessageIdEx")
    return messageId
}

def String getPropertyValue(String propertyName, MappingContext context) {
    def propertyValue = context.getProperty(propertyName)
    return propertyValue
}

def String getDefaultS4Id(String propertyName, MappingContext context) {
    def propertyValue = context.getProperty("P_DefaultS4ID")
    return propertyValue
}

def void filterIndustrySector(String[] is, Output output, MappingContext context) {
      
        String industryClassificationSystemCode = context.getProperty("P_IndustryClassificationSystemCode");
        for (int i = 0; i <is.length; i++) {
            if (is[i].equalsIgnoreCase(industryClassificationSystemCode))
                output.addValue("");
        }
        
}


def void filterIndustrySectorAttributes(String[] is, String[] os ,Output output, MappingContext context) {
      
        String industryClassificationSystemCode = context.getProperty("P_IndustryClassificationSystemCode");
        for (int i = 0; i <is.length; i++) {
            if (is[i].equalsIgnoreCase(industryClassificationSystemCode))
                output.addValue(os[i]);
    }
        
}