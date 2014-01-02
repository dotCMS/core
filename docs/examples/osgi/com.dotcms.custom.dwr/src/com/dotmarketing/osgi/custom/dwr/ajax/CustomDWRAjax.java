package com.dotmarketing.osgi.custom.dwr.ajax;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Nathan Keiter
 * Date: 12/05/13
 *
 */
public class CustomDWRAjax
{
	/**
	 * Usage:
	 * 
		<script type='text/javascript' src='/app/custom_dwr/engine.js'></script>
		<script type='text/javascript' src='/app/custom_dwr/util.js'></script>
		<script type='text/javascript' src='/app/custom_dwr/interface/CustomDWRAjax.js'></script>
		
		<script type='text/javascript'>
		
			function sayHelloDWR()
			{
				var name = 'Nathan Keiter';
				
				CustomDWRAjax.getHello( name, sayHelloCallback );
			}
			
			function sayHelloCallback( data )
			{
				if ( data[ "message" ] != null )
				{
					var messageData = data[ "message" ];
					
					alert( "DWR says: " + messageData );
				}
			}
			
			sayHelloDWR();
			
		</script>
	 * 
	 */
	public Map<String,Object> getHello( String name )
	{
		Map<String,Object> callbackData = new HashMap<String,Object>();
		
		callbackData.put( "message", "Hello, " + name );
		
		return callbackData;
	}
}
