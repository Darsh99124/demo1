
import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.json.JsonSlurper

def Message setCustomEmployeeMessageHeader(Message message) {
    
  def messageLog = messageLogFactory.getMessageLog(message);
	if(messageLog != null){
       
		def messageHeaderId = message.getProperties().get("p_msg_header_id")		
		if(messageHeaderId != null){
			messageLog.addCustomHeaderProperty("employeeS4Replication_msg_id", messageHeaderId)
        }
	}
	return message;
}

def Message setCustomEmployeeOrgAssignmentMessageHeader(Message message) {
    
  def messageLog = messageLogFactory.getMessageLog(message);
	if(messageLog != null){

		def messageHeaderId = message.getProperties().get("p_msg_header_id")	
		if(messageHeaderId != null){
			messageLog.addCustomHeaderProperty("employeeOrgAssignmentReplication_msg_id", messageHeaderId)	
        }
	}
	return message;
}