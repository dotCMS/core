<?xml version="1.0"?>
<!--
  Copyright 2004 Guy Van den Broeck

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="html" indent="yes"/>

<xsl:template match="/">
   <html>
      <head>
        <link href="css/diff.css" type="text/css" rel="stylesheet"/>
        <xsl:apply-templates select="diffreport/css/node()"/>
      </head>
      <body>
      
      <div class="diff-topbar">
        <table class="diffpage-html-firstlast">
        <tr>
        
        <td style="text-align: center; font-size: 140%;">
            
        </td>
        
        </tr></table>
         </div>
	     <xsl:apply-templates select="diffreport/diff/node()"/>
	  </body>
   </html>
</xsl:template>

<xsl:template match="@*|node()">
<xsl:copy>
  <xsl:apply-templates select="@*|node()"/>
</xsl:copy>
</xsl:template>

</xsl:stylesheet>
