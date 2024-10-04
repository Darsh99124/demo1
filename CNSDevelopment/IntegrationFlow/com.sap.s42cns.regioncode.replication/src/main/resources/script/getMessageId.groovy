import com.sap.it.api.mapping.*;

def String getMessageId(String p1, MappingContext context){
    String messageId = context.getHeader("SapMessageIdEx")
    return messageId
}
