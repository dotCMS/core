var prevLink;
var nextLink;
var itemsPerPage = 3;
var visiblePage = 0;
var items;
var clicks=0;


function paginate(num,id) {
	items = document.getElementsBySelector('div[id='+id+']');    
	itemsPerPage = num;
	prevLink = document.getElementById('prevLink');
	if(prevLink != null){
	prevLink.onclick=function() {
	 if(clicks >1){
	       clicks-=1;
	    }
	   if(visiblePage == 0){
		visiblePage = Math.min(Math.floor(items.length/itemsPerPage),items.length-1);
	
		}else{
		visiblePage = Math.max(0,visiblePage-1);
		}
		  
		displayPage();
		return false;
	}
	}
	nextLink = document.getElementById('nextLink');
	if(nextLink != null){
	nextLink.onclick=function() {
	
	visiblePage = Math.min(Math.floor(items.length/itemsPerPage),visiblePage+1);
	if(clicks <= visiblePage){
	    clicks+=1;
	   }
		if(visiblePage == items.length || clicks == Math.ceil(items.length/itemsPerPage)){
		   clicks = 0;
		   visiblePage = 0;
		}
		displayPage();
		return false;	
	}
	}
	
	displayPage();
}

function displayPage() {
	//itterate through all of the items setting style.display to 'none' for 
	//items that are below visiblePage+*itemsPerPage and above 
	//[(visiblePage+1)*itemsPerPage]-1 while setting those in between to 'block'
	for(i=0;i<items.length;i++){
		items[i].style.display='block';
		if (i<visiblePage*itemsPerPage||i>((visiblePage+1)*itemsPerPage)-1) {
			items[i].style.display='none';
		}
		
	}
}


