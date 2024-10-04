import com.sap.it.api.mapping.*;
import com.sap.gateway.ip.core.customdev.util.Message;
import java.lang.StringBuffer;



def String determineStatus(String FuncnlLocIsMarkedForDeletion,String FuncnlLocIsDeleted,String FunctionalLocationIsActive,String FuncnlLocIsDeactivated){
      if(FuncnlLocIsMarkedForDeletion == "true" || FuncnlLocIsDeleted == "true")
         return "OBSOLETE";
       else if (FuncnlLocIsDeactivated == "true") 
         return "BLOCKED";  
       else if (FunctionalLocationIsActive == "true")
         return "ACTIVE";
       else
         return "IN_PREPARATION";
}        

def String generateGuid(){
	return UUID.randomUUID().toString()
}

def String getBusinessSystemDisplayId(String arg1, MappingContext context){
    return context.getProperty(arg1)
}


 def String deleteLeadingZeroes(String str){
        StringBuffer sb = new StringBuffer(str);
        while (sb.length()>1 && sb.charAt(0) == '0')
            sb.deleteCharAt(0);
        return sb.toString();  // return in String
}





