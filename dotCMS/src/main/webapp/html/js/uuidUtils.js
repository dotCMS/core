
function isInodeSet(inode){
	    
	    if(inode==='SYSTEM_HOST' || inode==='SYSTEM_FOLDER'){
	    	return true;
	    }
	   
	    inode = inode + "";
		
		var validUUIDRegex = "^([a-f0-9]{8,8})\-([a-f0-9]{4,4})\-([a-f0-9]{4,4})\-([a-f0-9]{4,4})\-([a-f0-9]{12,12})$";
		var olderInodeRegex = "^[0-9]+$";

		if(inode.match(validUUIDRegex)){
			return true;
		}
		else if(inode.match(olderInodeRegex) && inode != '0'){			
			return true;
		}
		else{
			return false;
		}
		
}
