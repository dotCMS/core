
	<script type="text/javascript">
		function dotMakeBodVisible(){
			
			if(dojo.style(dojo.body(), "visibility") != "visible"){
				setTimeout( "dotMakeBodVisible()",3000);
				dojo.style(dojo.body(), "visibility", "visible");
				
			}

		}
		
		dojo.addOnLoad(dotMakeBodVisible);
		

	</script>
</body>
</html>
