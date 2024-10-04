import com.sap.it.api.mapping.*;
import java.util.stream.Collectors;

def String setSOAPMessageID(String arg1,  MappingContext context){
    context.setHeader("SapMessageIdEx", arg1)
	return arg1 
}

def String calculateChangeOrdinalNumberValue(String arg1){
    
   /* String changeOrdinalNumberValue =  arg1.toCharArray().collect().stream()
                .filter(e-> Character.isDigit(e))
                .limit(20)
                .map(String::valueOf)
                .collect(Collectors.joining())*/
                
    String changeOrdinalNumberValue = arg1.replace("T", "")
                .replace("Z", "")
                .replace("-", "")
                .replace(":", "")
                .replace(".", "")

    if (changeOrdinalNumberValue.length() >=20)
            return changeOrdinalNumberValue.substring(0,20)
    else
        return changeOrdinalNumberValue            
    
}