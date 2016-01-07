
	<script type="text/javascript">
		function dotMakeBodVisible(){
			if(document.location.href.indexOf("#")<0){
				dojo.style(dojo.body(), "visibility", "visible");
			}
		}
		
		dojo.addOnLoad(dotMakeBodVisible);
		setTimeout( "dotMakeBodVisible",2000);
	
	</script>
</body>
</html>
