function showDiv(id)
{

	if(document.getElementById(id).style.display == 'none')
	{
		document.getElementById(id).style.display = "";
	} 
	else 
	{
		document.getElementById(id).style.display = "none";
    }
}

function checkFormSendToFriend()
{
  	if(document.getElementById('send_to_friend_username').value=='')
	{
  		alert('Your Name is required!!');
  		return false;
  	}
  	if(document.getElementById('send_to_friend_from').value=='')
	{
  		alert('Your Email Address is required!!');
  		return false;
  	} 
	else 
	{
  		if(!emailValid(document.getElementById('send_to_friend_from'), 'Email Address'))
		{
  		  return false;
  		}
  	
  	}
  	if(document.getElementById('send_to_friend_to').value==''){
  		alert('Recipient Email Address is required!!');
  		return false;
  	} else {
  		if(!emailValid(document.getElementById('send_to_friend_to'), 'Recipient Email Address')){
  		  return false;
  		}
  	}
  	return true;
}