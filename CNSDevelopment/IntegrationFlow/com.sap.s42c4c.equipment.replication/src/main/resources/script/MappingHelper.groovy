import com.sap.it.api.mapping.*;


def String generateGuid(String arg1){
	return UUID.randomUUID()
}

def String generateMessageHeaderId(String arg1, MappingContext context){
  	String messageHeaderId =  UUID.randomUUID().toString()
  	context.setProperty("P_MessageHeaderId", messageHeaderId)
  	return messageHeaderId
  }

def String getBusinessSystemDisplayId(String arg1, MappingContext context){
    return context.getProperty(arg1)
}

def String getStatus(String EquipmentIsMarkedForDeletion,String EquipmentIsAtCustomer,String EquipmentIsAvailable,String EquipmentIsInWarehouse,String EquipmentIsAssignedToDelivery,String EquipmentIsInstalled,String EquipIsAllocToSuperiorEquip,String EquipmentIsInactive) {

    if(EquipmentIsMarkedForDeletion == "true")
        return "OBSOLETE";
     
    else if (EquipmentIsAtCustomer == "true" || EquipmentIsAvailable == "true" || EquipmentIsInWarehouse == "true" || EquipmentIsAssignedToDelivery == "true" || EquipmentIsInstalled == "true" || EquipIsAllocToSuperiorEquip == "true" || EquipmentIsInactive == "false") 
        return "ACTIVE";
    
    else 
        return "IN_PREPARATION";
}

def String getLanguage(String arg1, MappingContext context){
    return context.getProperty("P_Language")
}