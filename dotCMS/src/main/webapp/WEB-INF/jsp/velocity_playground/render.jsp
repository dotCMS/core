<%@ page import="com.liferay.portal.language.LanguageUtil"%>
<%@ page import="com.dotcms.enterprise.LicenseUtil" %>
<%@page import="com.dotcms.enterprise.license.LicenseLevel"%>


<%if( LicenseUtil.getLevel() < LicenseLevel.STANDARD.level){ %>
    <div class="portlet-wrapper">
        <jsp:include page="/WEB-INF/jsp/es-search/not_licensed.jsp"></jsp:include>
    </div>
<%return;}%>

<script src="/html/js/ace-builds-1.2.3/src-noconflict/ace.js" type="text/javascript"></script>
<script>



    const velocityHistory = [];

    //localStorage.removeItem('velocityPlayground');
    function addToHistory(velocityQuery){

        for( i=0;i<velocityHistory.length;i++){
            if(velocityHistory[i] == velocityQuery){
                velocityHistory.splice(i, 1);
            }
        }

        velocityHistory.unshift(velocityQuery);
        if(velocityHistory.length>10){
            velocityHistory.pop();
        }

        window.localStorage.setItem('velocityPlayground',JSON.stringify(velocityHistory));
    }

    function updateOptions(){
        var select =  document.getElementById("historySelect")

         for (var i=0; i<select.length; i++) {
             select.remove(i);
         }

        for( i=0;i<velocityHistory.length;i++){
            var opt = document.createElement('option');
            opt.value = i;
            opt.innerHTML = "history " + (i + 1);
            select.appendChild(opt);
        }
    }
    
    function selectOption(){
        var select =  document.getElementById("historySelect")
        editor.setValue(velocityHistory[select.selectedIndex]);
    }
    
    
    
    
    var editor;
    function initAce() {

        ace.config.set('basePath', '/html/js/ace-builds-1.2.3/src-noconflict/');
        editor = ace.edit('esEditor');
        editor.setTheme("ace/theme/tomorrow_night");
        editor.getSession().setMode("ace/mode/velocity");
        
        let myLocal = window.localStorage.getItem('velocityPlayground'); 
        myLocal = (myLocal) ? JSON.parse(myLocal) : {};

        if(myLocal && myLocal.length>0){
            for( i=0;i<myLocal.length;i++){
                velocityHistory[i]=myLocal[i];
            }
            
            editor.setValue(velocityHistory[0]);
        }
   
        updateOptions();
        handleWrapMode();
        
    }

    function runVelocity(){
        var code = editor.getValue().trim();
        
        addToHistory(code);
        document.getElementById("secondCode").innerHTML="";
        document.getElementById("spinner").style.display="inline-block";

        
        code="#set($dotTimer = $date.date.time)"  + code;
        
        code+="\n--\n$math.sub($date.date.time, $dotTimer)ms";
        
        
        fetch('/api/vtl/dynamic/', {
            method: 'POST', 
            headers: {
              'Content-Type': 'text/plain',
            },
            body: code,
          }).then((response) => response.text())
          .then((data) => {
              document.getElementById("spinner").style.display="none";
              document.getElementById("secondCode").innerText=data.replace(/^\s+|\s+$/g, '');
           })
                 

    }


    function handleWrapMode() {

        editor.getSession().setUseWrapMode(false);
        
        if(document.getElementById("wrapEditorCheckbox").checked){

            editor.getSession().setUseWrapMode(true);
        }
        
    }

    dojo.addOnLoad(initAce);
    

    let ismdwn = 0
    separator.addEventListener('mousedown', mD)

    function mD(event) {
      ismdwn = 1
      document.body.addEventListener('mousemove', mV)
      document.body.addEventListener('mouseup', end)
    }

    function mV(event) {
      if (ismdwn === 1) {
          first.style.flexBasis = event.clientX + "px";
          handleWrapMode();
      } else {
        end()
      }
    }
    const end = (e) => {
      ismdwn = 0
      document.body.removeEventListener('mouseup', end)
      separator.removeEventListener('mousemove', mV)
    }

    
    
   
</script>
<style type="text/css" media="screen">

body{
    width:100%;
    overflow-x:hidden;
    overflow-y:hidden;
}


#topBar{
    padding:10px;
    width:50%;
    height:50px;
}

#esEditor {
    width: 100%;
    height: 100%;
    border: 1px solid #eeeeee;
}

.splitter {
    position:absolute;
    top:55px;
    bottom:0px;
    width: 100%;
    display: flex;
}

#separator {
    display: flex;
    cursor: col-resize;
    background-color: #eeeeee;
    background-image:
        url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='10' height='30'><path d='M2 0 v30 M5 0 v30 M8 0 v30' fill='none' stroke='black'/></svg>");
    background-repeat: no-repeat;
    background-position: center;
    flex-grow: 0;
    flex-shrink: 0;
    width: 10px;
}

#first {
    display: flex;
    background-color: #fff;
    flex-grow: 1;
    flex-shrink: 0;
    flex-basis: 50%; /* initial status */
    width: 100%;
}

#second {
    display: flex;
    position: relative;
    width: 100%;
    background-color: #eee;
    flex-grow: 0;
    flex-shrink: 1;
    padding: 20px;
    color: #fff;
    background-color: #222;
    white-space: pre;
    text-shadow: 0 1px 0 #000;
    font: 14px/24px 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', 'source-code-pro', monospace;
    overflow-x:auto;
    overflow-y:visible; 

}

#spinner{

  width:50px;height:50px;
  position:absolute;
  top: 30%;
  left: 40%;
  transform: translate(-50%, -50%);
  text-align: center;
  display:none;
  
}

</style>



<div id="topBar">
    <input type="checkbox" id="wrapEditorCheckbox" name="wrapEditor" value="true" onclick="handleWrapMode()" checked="true"/> <label for="wrapEditorCheckbox"><%= LanguageUtil.get(pageContext, "Wrap-Code") %></label> &nbsp; &nbsp; 

    <select id="historySelect" class="dijitTextBox  dijitInputField" placeholder="History" style="width:150px" onchange="selectOption()">
        <option value="History">History</option>
    </select>
   <div style="float:right">
       <button type="button" id="submitButton" iconClass="queryIcon" onClick="runVelocity()" dojoType="dijit.form.Button"><%=LanguageUtil.get(pageContext, "Run Velocity")%> &rarr;</button>
   </div>
</div>
<div class="splitter">
    <div id="first">
         <div id="esEditor"></div>
    </div>
    <div id="separator"></div>
    <div id="second">
        <div id="secondCode"></div>
        <div id="spinner">
        
         <svg viewBox="0 0 38 38" xmlns="http://www.w3.org/2000/svg">
             <defs>
                 <linearGradient x1="8.042%" y1="0%" x2="65.682%" y2="23.865%" id="a">
                     <stop stop-color="#fff" stop-opacity="0" offset="0%"/>
                     <stop stop-color="#fff" stop-opacity=".631" offset="63.146%"/>
                     <stop stop-color="#fff" offset="100%"/>
                 </linearGradient>
             </defs>
             <g fill="none" fill-rule="evenodd">
                 <g transform="translate(1 1)">
                     <path d="M36 18c0-9.94-8.06-18-18-18" id="Oval-2" stroke="url(#a)" stroke-width="2">
                         <animateTransform
                             attributeName="transform"
                             type="rotate"
                             from="0 18 18"
                             to="360 18 18"
                             dur="0.9s"
                             repeatCount="indefinite" />
                     </path>
                     <circle fill="#fff" cx="36" cy="18" r="1">
                         <animateTransform
                             attributeName="transform"
                             type="rotate"
                             from="0 18 18"
                             to="360 18 18"
                             dur="0.9s"
                             repeatCount="indefinite" />
                     </circle>
                 </g>
             </g>
         </svg>

        
        
        
        </div>
    </div>
</div>








