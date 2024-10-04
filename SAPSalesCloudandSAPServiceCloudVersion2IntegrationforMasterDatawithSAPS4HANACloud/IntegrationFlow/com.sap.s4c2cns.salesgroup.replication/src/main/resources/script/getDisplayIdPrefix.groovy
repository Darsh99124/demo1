import com.sap.it.api.mapping.*;

def String getExchangeProperty(String propertyName, MappingContext context) {
    return context.getProperty(propertyName)
}