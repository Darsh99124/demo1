import com.sap.it.api.mapping.*;
import java.util.UUID;
import java.util.List;

/*Add MappingContext parameter to read or set headers and properties
def String customFunc1(String P1,String P2,MappingContext context) {
         String value1 = context.getHeader(P1);
         String value2 = context.getProperty(P2);
         return value1+value2;
}

Add Output parameter to assign the output value.
def void custFunc2(String[] is,String[] ps, Output output, MappingContext context) {
        String value1 = context.getHeader(is[0]);
        String value2 = context.getProperty(ps[0]);
        output.addValue(value1);
        output.addValue(value2);
}*/

def String getId(String arg1){
	return UUID.randomUUID().toString()
}

def String getMessageId(String p1, MappingContext context){
    String messageId = context.getHeader("SapMessageIdEx")
    return messageId
}

def String getParentDisplayId(String materialGroupDisplayId, MappingContext context) {

  String pattern = context.getProperty("p_MaterialGroupPattern")
  if (pattern.isEmpty()) return ""
  String[] patternList = pattern.split("-")
  int[] index = new int[patternList.length]
  index[0] = patternList[0].length()
  for (int i = 1; i < patternList.length; i++)
    index[i] = index[i - 1] + patternList[i].length()

  int groupLength = materialGroupDisplayId.length()
  if (groupLength == index[0])
    return ""
  else {
    int matchingPatternIndex = 0
    for (int i = 1; i < index.length; i++)
      if (groupLength == index[i]) {
        matchingPatternIndex = i
        break
      }
    if (matchingPatternIndex == 0)
      throw new RuntimeException("Material Group " + materialGroupDisplayId + " does not match pattern " + pattern)
    else
      return materialGroupDisplayId.substring(0, index[matchingPatternIndex - 1])
  }
}