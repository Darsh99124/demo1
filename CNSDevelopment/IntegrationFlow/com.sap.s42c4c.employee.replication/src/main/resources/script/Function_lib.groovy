import com.sap.it.api.mapping.*;


def String getIntValue(String value){
	return Integer.parseInt(value)
}

def String getMessageId(String value, MappingContext context){
    String messageId = context.getHeader("SapMessageIdEx")
    return messageId
}

def String getdefaultS4ID(String value, MappingContext context){
	return context.getProperty(p1);
}

def String generateUUID(String value){
	return UUID.randomUUID().toString()
}

def String generateMessageHeaderUUID(String value, MappingContext context){
    def messageId = UUID.randomUUID().toString()
    context.setProperty ("p_msg_header_id", messageId)
	return messageId
}