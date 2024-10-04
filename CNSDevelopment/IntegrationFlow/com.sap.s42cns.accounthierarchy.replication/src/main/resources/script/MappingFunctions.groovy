import com.sap.gateway.ip.core.customdev.util.Message;
import java.util.HashMap;
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import com.sap.it.api.mapping.*;
import com.sap.it.api.mapping.MappingContext




def String generateUuid(String arg1){
	return UUID.randomUUID().toString()
}

def String getUsageCode(String propertyName, MappingContext context) {
    def propertyValue = context.getProperty(propertyName);
    return propertyValue;
}


def String removeLeadingZeros(String arg1){
	return arg1.replaceFirst('^0+(?!$)', "")
}
