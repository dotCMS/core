function writeCountriesSelect(fieldName, currentVal) {
	writeCountriesSelect(fieldName, currentVal, false);
}

function writeCountriesSelect(fieldName, currentVal, isDojoComboBox) {
	if (isDojoComboBox)
		document.write('<select name="'+fieldName+'" id="'+fieldName+'" style="width:200px" dojoType="dijit.form.FilteringSelect" autocomplete="false" value="' + currentVal + '">');
	else
		document.write('<select name="'+fieldName+'" id="'+fieldName+'" style="width:200px">');
	document.write('<option value="">Select Below</option>');
	document.write('<option value="United States of America" ' + ((currentVal == "United States of America")?'selected="true"':'') + '> United States of America</option>');
	document.write('<option value="Afghanistan" ' + ((currentVal == "Afghanistan")?'selected="true"':'') + '> Afghanistan</option>');
	document.write('<option value="Albania" ' + ((currentVal == "Albania")?'selected="true"':'') + '> Albania</option>');
	document.write('<option value="Algeria" ' + ((currentVal == "Algeria")?'selected="true"':'') + '> Algeria</option>');
	document.write('<option value="Andorra" ' + ((currentVal == "Andorra")?'selected="true"':'') + '> Andorra</option>');
	document.write('<option value="Angola" ' + ((currentVal == "Angola")?'selected="true"':'') + '> Angola</option>');
	document.write('<option value="Anguilla" ' + ((currentVal == "Anguilla")?'selected="true"':'') + '> Anguilla</option>');
	document.write('<option value="Antigua and Barbuda" ' + ((currentVal == "Antigua and Barbuda")?'selected="true"':'') + '> Antigua and Barbuda</option>');
	document.write('<option value="Argentina" ' + ((currentVal == "Argentina")?'selected="true"':'') + '> Argentina</option>');
	document.write('<option value="Armenia" ' + ((currentVal == "Armenia")?'selected="true"':'') + '> Armenia</option>');
	document.write('<option value="Aruba" ' + ((currentVal == "Aruba")?'selected="true"':'') + '> Aruba</option>');
	document.write('<option value="Australia" ' + ((currentVal == "Australia")?'selected="true"':'') + '> Australia</option>');
	document.write('<option value="Austria" ' + ((currentVal == "Austria")?'selected="true"':'') + '> Austria</option>');
	document.write('<option value="Azerbaijan" ' + ((currentVal == "Azerbaijan")?'selected="true"':'') + '> Azerbaijan</option>');
	document.write('<option value="Azores" ' + ((currentVal == "Azores")?'selected="true"':'') + '> Azores</option>');
	document.write('<option value="Bahamas" ' + ((currentVal == "Bahamas")?'selected="true"':'') + '> Bahamas</option>');
	document.write('<option value="Bahrain" ' + ((currentVal == "Bahrain")?'selected="true"':'') + '> Bahrain</option>');
	document.write('<option value="Bangladesh" ' + ((currentVal == "Bangladesh")?'selected="true"':'') + '> Bangladesh</option>');
	document.write('<option value="Barbados" ' + ((currentVal == "Barbados")?'selected="true"':'') + '> Barbados</option>');
	document.write('<option value="Belgium" ' + ((currentVal == "Belgium")?'selected="true"':'') + '> Belgium</option>');
	document.write('<option value="Belize" ' + ((currentVal == "Belize")?'selected="true"':'') + '> Belize</option>');
	document.write('<option value="Benin" ' + ((currentVal == "Benin")?'selected="true"':'') + '> Benin</option>');
	document.write('<option value="Bermuda" ' + ((currentVal == "Bermuda")?'selected="true"':'') + '> Bermuda</option>');
	document.write('<option value="Bhutan" ' + ((currentVal == "Bhutan")?'selected="true"':'') + '> Bhutan</option>');
	document.write('<option value="Bolivia" ' + ((currentVal == "Bolivia")?'selected="true"':'') + '> Bolivia</option>');
	document.write('<option value="Bosnia and Herzegovina" ' + ((currentVal == "Bosnia and Herzegovina")?'selected="true"':'') + '> Bosnia and Herzegovina</option>');
	document.write('<option value="Botswana" ' + ((currentVal == "Botswana")?'selected="true"':'') + '> Botswana</option>');
	document.write('<option value="Brazil" ' + ((currentVal == "Brazil")?'selected="true"':'') + '> Brazil</option>');
	document.write('<option value="British Virgin Islands" ' + ((currentVal == "British Virgin Islands")?'selected="true"':'') + '> British Virgin Islands</option>');
	document.write('<option value="Brunei Darussalam" ' + ((currentVal == "Brunei Darussalam")?'selected="true"':'') + '> Brunei Darussalam</option>');
	document.write('<option value="Bulgaria" ' + ((currentVal == "Bulgaria")?'selected="true"':'') + '> Bulgaria</option>');
	document.write('<option value="Myanmar" ' + ((currentVal == "Myanmar")?'selected="true"':'') + '> Myanmar</option>');
	document.write('<option value="Burundi" ' + ((currentVal == "Burundi")?'selected="true"':'') + '> Burundi</option>');
	document.write('<option value="Belarus" ' + ((currentVal == "Belarus")?'selected="true"':'') + '> Belarus</option>');
	document.write('<option value="Cameroon" ' + ((currentVal == "Cameroon")?'selected="true"':'') + '> Cameroon</option>');
	document.write('<option value="Canada" ' + ((currentVal == "Canada")?'selected="true"':'') + '> Canada</option>');
	document.write('<option value="Canary Islands" ' + ((currentVal == "Canary Islands")?'selected="true"':'') + '> Canary Islands</option>');
	document.write('<option value="Cape Verde" ' + ((currentVal == "Cape Verde")?'selected="true"':'') + '> Cape Verde</option>');
	document.write('<option value="Fed. States of Micronesia" ' + ((currentVal == "Fed. States of Micronesia")?'selected="true"':'') + '> Fed. States of Micronesia</option>');
	document.write('<option value="Cayman Islands" ' + ((currentVal == "Cayman Islands")?'selected="true"':'') + '> Cayman Islands</option>');
	document.write('<option value="Central African Republic" ' + ((currentVal == "Central African Republic")?'selected="true"':'') + '> Central African Republic</option>');
	document.write('<option value="Chad" ' + ((currentVal == "Chad")?'selected="true"':'') + '> Chad</option>');
	document.write('<option value="Chile" ' + ((currentVal == "Chile")?'selected="true"':'') + '> Chile</option>');
	document.write('<option value="Colombia" ' + ((currentVal == "Colombia")?'selected="true"':'') + '> Colombia</option>');
	document.write('<option value="Comoras" ' + ((currentVal == "Comoras")?'selected="true"':'') + '> Comoras</option>');
	document.write('<option value="Congo" ' + ((currentVal == "Congo")?'selected="true"':'') + '> Congo</option>');
	document.write('<option value="Cook Islands" ' + ((currentVal == "Cook Islands")?'selected="true"':'') + '> Cook Islands</option>');
	document.write('<option value="Costa Rica" ' + ((currentVal == "Costa Rica")?'selected="true"':'') + '> Costa Rica</option>');
	document.write('<option value="Croatia" ' + ((currentVal == "Croatia")?'selected="true"':'') + '> Croatia</option>');
	document.write('<option value="Cuba" ' + ((currentVal == "Cuba")?'selected="true"':'') + '> Cuba</option>');
	document.write('<option value="Cyprus" ' + ((currentVal == "Cyprus")?'selected="true"':'') + '> Cyprus</option>');
	document.write('<option value="Czech Republic" ' + ((currentVal == "Czech Republic")?'selected="true"':'') + '> Czech Republic</option>');
	document.write('<option value="Denmark" ' + ((currentVal == "Denmark")?'selected="true"':'') + '> Denmark</option>');
	document.write('<option value="Djibouti" ' + ((currentVal == "Djibouti")?'selected="true"':'') + '> Djibouti</option>');
	document.write('<option value="Dominica Commonwealth of" ' + ((currentVal == "Dominica Commonwealth of")?'selected="true"':'') + '> Dominica Commonwealth of</option>');
	document.write('<option value="Dominican Republic" ' + ((currentVal == "Dominican Republic")?'selected="true"':'') + '> Dominican Republic</option>');
	document.write('<option value="Ecuador" ' + ((currentVal == "Ecuador")?'selected="true"':'') + '> Ecuador</option>');
	document.write('<option value="Egypt" ' + ((currentVal == "Egypt")?'selected="true"':'') + '> Egypt</option>');
	document.write('<option value="El Salvador" ' + ((currentVal == "El Salvador")?'selected="true"':'') + '> El Salvador</option>');
	document.write('<option value="England" ' + ((currentVal == "England")?'selected="true"':'') + '> England</option>');
	document.write('<option value="Eritrea" ' + ((currentVal == "Eritrea")?'selected="true"':'') + '> Eritrea</option>');
	document.write('<option value="Equatorial Guinea" ' + ((currentVal == "Equatorial Guinea")?'selected="true"':'') + '> Equatorial Guinea</option>');
	document.write('<option value="Estonia" ' + ((currentVal == "Estonia")?'selected="true"':'') + '> Estonia</option>');
	document.write('<option value="Ethiopia" ' + ((currentVal == "Ethiopia")?'selected="true"':'') + '> Ethiopia</option>');
	document.write('<option value="Fiji" ' + ((currentVal == "Fiji")?'selected="true"':'') + '> Fiji</option>');
	document.write('<option value="Finland" ' + ((currentVal == "Finland")?'selected="true"':'') + '> Finland</option>');
	document.write('<option value="France" ' + ((currentVal == "France")?'selected="true"':'') + '> France</option>');
	document.write('<option value="French Polynesia" ' + ((currentVal == "French Polynesia")?'selected="true"':'') + '> French Polynesia</option>');
	document.write('<option value="French Guiana" ' + ((currentVal == "French Guiana")?'selected="true"':'') + '> French Guiana</option>');
	document.write('<option value="Gabon" ' + ((currentVal == "Gabon")?'selected="true"':'') + '> Gabon</option>');
	document.write('<option value="Gambia The" ' + ((currentVal == "Gambia The")?'selected="true"':'') + '> Gambia</option>');
	document.write('<option value="Georgia" ' + ((currentVal == "Georgia")?'selected="true"':'') + '> Georgia</option>');
	document.write('<option value="Germany" ' + ((currentVal == "Germany")?'selected="true"':'') + '> Germany</option>');
	document.write('<option value="Ghana" ' + ((currentVal == "Ghana")?'selected="true"':'') + '> Ghana</option>');
	document.write('<option value="Gibraltar" ' + ((currentVal == "Gibraltar")?'selected="true"':'') + '> Gibraltar</option>');
	document.write('<option value="Greece" ' + ((currentVal == "Greece")?'selected="true"':'') + '> Greece</option>');
	document.write('<option value="Greenland" ' + ((currentVal == "Greenland")?'selected="true"':'') + '> Greenland</option>');
	document.write('<option value="Grenada" ' + ((currentVal == "Grenada")?'selected="true"':'') + '> Grenada</option>');
	document.write('<option value="Guadeloupe" ' + ((currentVal == "Guadeloupe")?'selected="true"':'') + '> Guadeloupe</option>');
	document.write('<option value="Guatemala" ' + ((currentVal == "Guatemala")?'selected="true"':'') + '> Guatemala</option>');
	document.write('<option value="Guinea" ' + ((currentVal == "Guinea")?'selected="true"':'') + '> Guinea</option>');
	document.write('<option value="Guinea Bissau" ' + ((currentVal == "Guinea Bissau")?'selected="true"':'') + '> Guinea Bissau</option>');
	document.write('<option value="Guyana" ' + ((currentVal == "Guyana")?'selected="true"':'') + '> Guyana</option>');
	document.write('<option value="Haiti" ' + ((currentVal == "Haiti")?'selected="true"':'') + '> Haiti</option>');
	document.write('<option value="Honduras" ' + ((currentVal == "Honduras")?'selected="true"':'') + '> Honduras</option>');
	document.write('<option value="Hong Kong" ' + ((currentVal == "Hong Kong")?'selected="true"':'') + '> Hong Kong</option>');
	document.write('<option value="Hungary" ' + ((currentVal == "Hungary")?'selected="true"':'') + '> Hungary</option>');
	document.write('<option value="Iceland" ' + ((currentVal == "Iceland")?'selected="true"':'') + '> Iceland</option>');
	document.write('<option value="India" ' + ((currentVal == "India")?'selected="true"':'') + '> India</option>');
	document.write('<option value="Indonesia" ' + ((currentVal == "Indonesia")?'selected="true"':'') + '> Indonesia</option>');
	document.write('<option value="Iran" ' + ((currentVal == "Iran")?'selected="true"':'') + '> Iran</option>');
	document.write('<option value="Iraq" ' + ((currentVal == "Iraq")?'selected="true"':'') + '> Iraq</option>');
	document.write('<option value="Ireland" ' + ((currentVal == "Ireland")?'selected="true"':'') + '> Ireland</option>');
	document.write('<option value="Isle of Man" ' + ((currentVal == "Isle of Man")?'selected="true"':'') + '> Isle of Man</option>');
	document.write('<option value="Israel" ' + ((currentVal == "Israel")?'selected="true"':'') + '> Israel</option>');
	document.write('<option value="Italy" ' + ((currentVal == "Italy")?'selected="true"':'') + '> Italy</option>');
	document.write('<option value="Cote D\'Ivoire-Ivory Coast" ' + ((currentVal == "Cote D\'Ivoire-Ivory Coast")?'selected="true"':'') + '> Cote D\'Ivoire-Ivory Coast</option>');
	document.write('<option value="Jamaica" ' + ((currentVal == "Jamaica")?'selected="true"':'') + '> Jamaica</option>');
	document.write('<option value="Japan" ' + ((currentVal == "Japan")?'selected="true"':'') + '> Japan</option>');
	document.write('<option value="Jordan" ' + ((currentVal == "Jordan")?'selected="true"':'') + '> Jordan</option>');
	document.write('<option value="Cambodia" ' + ((currentVal == "Cambodia")?'selected="true"':'') + '> Cambodia</option>');
	document.write('<option value="Kazakstan" ' + ((currentVal == "Kazakstan")?'selected="true"':'') + '> Kazakstan</option>');
	document.write('<option value="Kenya" ' + ((currentVal == "Kenya")?'selected="true"':'') + '> Kenya</option>');
	document.write('<option value="Kiribati" ' + ((currentVal == "Kiribati")?'selected="true"':'') + '> Kiribati</option>');
	document.write('<option value="Korea (DPR)" ' + ((currentVal == "Korea (DPR)")?'selected="true"':'') + '> Korea (DPR)</option>');
	document.write('<option value="Korea(ROK)" ' + ((currentVal == "Korea(ROK)")?'selected="true"':'') + '> Korea(ROK)</option>');
	document.write('<option value="Kuwait" ' + ((currentVal == "Kuwait")?'selected="true"':'') + '> Kuwait</option>');
	document.write('<option value="Kyrgyzstan" ' + ((currentVal == "Kyrgyzstan")?'selected="true"':'') + '> Kyrgyzstan</option>');
	document.write('<option value="Laos" ' + ((currentVal == "Laos")?'selected="true"':'') + '> Laos</option>');
	document.write('<option value="Latvia" ' + ((currentVal == "Latvia")?'selected="true"':'') + '> Latvia</option>');
	document.write('<option value="Lebanon" ' + ((currentVal == "Lebanon")?'selected="true"':'') + '> Lebanon</option>');
	document.write('<option value="Lesotho" ' + ((currentVal == "Lesotho")?'selected="true"':'') + '> Lesotho</option>');
	document.write('<option value="Liberia" ' + ((currentVal == "Liberia")?'selected="true"':'') + '> Liberia</option>');
	document.write('<option value="Libya" ' + ((currentVal == "Libya")?'selected="true"':'') + '> Libya</option>');
	document.write('<option value="Liechtenstein" ' + ((currentVal == "Liechtenstein")?'selected="true"':'') + '> Liechtenstein</option>');
	document.write('<option value="Lithuania" ' + ((currentVal == "Lithuania")?'selected="true"':'') + '> Lithuania</option>');
	document.write('<option value="Luxembourg" ' + ((currentVal == "Luxembourg")?'selected="true"':'') + '> Luxembourg</option>');
	document.write('<option value="Macau" ' + ((currentVal == "Macau")?'selected="true"':'') + '> Macau</option>');
	document.write('<option value="Macedonia(F.Y.R.O.M.)" ' + ((currentVal == "Macedonia(F.Y.R.O.M.)")?'selected="true"':'') + '> Macedonia(F.Y.R.O.M.)</option>');
	document.write('<option value="Madagascar" ' + ((currentVal == "Madagascar")?'selected="true"':'') + '> Madagascar</option>');
	document.write('<option value="Madeira Islands" ' + ((currentVal == "Madeira Islands")?'selected="true"':'') + '> Madeira Islands</option>');
	document.write('<option value="Malawi" ' + ((currentVal == "Malawi")?'selected="true"':'') + '> Malawi</option>');
	document.write('<option value="Malaysia" ' + ((currentVal == "Malaysia")?'selected="true"':'') + '> Malaysia</option>');
	document.write('<option value="Maldives" ' + ((currentVal == "Maldives")?'selected="true"':'') + '> Maldives</option>');
	document.write('<option value="Mali" ' + ((currentVal == "Mali")?'selected="true"':'') + '> Mali</option>');
	document.write('<option value="Malta" ' + ((currentVal == "Malta")?'selected="true"':'') + '> Malta</option>');
	document.write('<option value="Martinique" ' + ((currentVal == "Martinique")?'selected="true"':'') + '> Martinique</option>');
	document.write('<option value="Marshall Islands" ' + ((currentVal == "Marshall Islands")?'selected="true"':'') + '> Marshall Islands</option>');
	document.write('<option value="Mauritania" ' + ((currentVal == "Mauritania")?'selected="true"':'') + '> Mauritania</option>');
	document.write('<option value="Mauritius" ' + ((currentVal == "Mauritius")?'selected="true"':'') + '> Mauritius</option>');
	document.write('<option value="Mexico" ' + ((currentVal == "Mexico")?'selected="true"':'') + '> Mexico</option>');
	document.write('<option value="Moldova" ' + ((currentVal == "Moldova")?'selected="true"':'') + '> Moldova</option>');
	document.write('<option value="Midway Islands" ' + ((currentVal == "Midway Islands")?'selected="true"':'') + '> Midway Islands</option>');
	document.write('<option value="Monaco" ' + ((currentVal == "Monaco")?'selected="true"':'') + '> Monaco</option>');
	document.write('<option value="Mongolia" ' + ((currentVal == "Mongolia")?'selected="true"':'') + '> Mongolia</option>');
	document.write('<option value="Morocco" ' + ((currentVal == "Morocco")?'selected="true"':'') + '> Morocco</option>');
	document.write('<option value="Montserrat" ' + ((currentVal == "Montserrat")?'selected="true"':'') + '> Montserrat</option>');
	document.write('<option value="Mozambique" ' + ((currentVal == "Mozambique")?'selected="true"':'') + '> Mozambique</option>');
	document.write('<option value="Nauru" ' + ((currentVal == "Nauru")?'selected="true"':'') + '> Nauru</option>');
	document.write('<option value="Nepal" ' + ((currentVal == "Nepal")?'selected="true"':'') + '> Nepal</option>');
	document.write('<option value="Namibia" ' + ((currentVal == "Namibia")?'selected="true"':'') + '> Namibia</option>');
	document.write('<option value="Netherlands" ' + ((currentVal == "Netherlands")?'selected="true"':'') + '> Netherlands</option>');
	document.write('<option value="Netherlands Antilles" ' + ((currentVal == "Netherlands Antilles")?'selected="true"':'') + '> Netherlands Antilles</option>');
	document.write('<option value="New Caledonia" ' + ((currentVal == "New Caledonia")?'selected="true"':'') + '> New Caledonia</option>');
	document.write('<option value="Papua New Guinea" ' + ((currentVal == "Papua New Guinea")?'selected="true"':'') + '> Papua New Guinea</option>');
	document.write('<option value="New Zealand" ' + ((currentVal == "New Zealand")?'selected="true"':'') + '> New Zealand</option>');
	document.write('<option value="Nicaragua" ' + ((currentVal == "Nicaragua")?'selected="true"':'') + '> Nicaragua</option>');
	document.write('<option value="Niger" ' + ((currentVal == "Niger")?'selected="true"':'') + '> Niger</option>');
	document.write('<option value="Nigeria" ' + ((currentVal == "Nigeria")?'selected="true"':'') + '> Nigeria</option>');
	document.write('<option value="Niue Island" ' + ((currentVal == "Niue Island")?'selected="true"':'') + '> Niue Island</option>');
	document.write('<option value="Northern Ireland" ' + ((currentVal == "Northern Ireland")?'selected="true"':'') + '> Northern Ireland</option>');
	document.write('<option value="Norway" ' + ((currentVal == "Norway")?'selected="true"':'') + '> Norway</option>');
	document.write('<option value="Okinawa" ' + ((currentVal == "Okinawa")?'selected="true"':'') + '> Okinawa</option>');
	document.write('<option value="Oman" ' + ((currentVal == "Oman")?'selected="true"':'') + '> Oman</option>');
	document.write('<option value="Pakistan" ' + ((currentVal == "Pakistan")?'selected="true"':'') + '> Pakistan</option>');
	document.write('<option value="Palau" ' + ((currentVal == "Palau")?'selected="true"':'') + '> Palau</option>');
	document.write('<option value="Panama" ' + ((currentVal == "Panama")?'selected="true"':'') + '> Panama</option>');
	document.write('<option value="Paraguay" ' + ((currentVal == "Paraguay")?'selected="true"':'') + '> Paraguay</option>');
	document.write('<option value="China Peoples Republic Of" ' + ((currentVal == "China Peoples Republic Of")?'selected="true"':'') + '> China Peoples Republic Of</option>');
	document.write('<option value="Peru" ' + ((currentVal == "Peru")?'selected="true"':'') + '> Peru</option>');
	document.write('<option value="Philippines" ' + ((currentVal == "Philippines")?'selected="true"':'') + '> Philippines</option>');
	document.write('<option value="Poland" ' + ((currentVal == "Poland")?'selected="true"':'') + '> Poland</option>');
	document.write('<option value="Portugal" ' + ((currentVal == "Portugal")?'selected="true"':'') + '> Portugal</option>');
	document.write('<option value="Qatar" ' + ((currentVal == "Qatar")?'selected="true"':'') + '> Qatar</option>');
	document.write('<option value="Zimbabwe" ' + ((currentVal == "Zimbabwe")?'selected="true"':'') + '> Zimbabwe</option>');
	document.write('<option value="Reunion" ' + ((currentVal == "Reunion")?'selected="true"':'') + '> Reunion</option>');
	document.write('<option value="Romania" ' + ((currentVal == "Romania")?'selected="true"':'') + '> Romania</option>');
	document.write('<option value="Russia" ' + ((currentVal == "Russia")?'selected="true"':'') + '> Russia</option>');
	document.write('<option value="St. Kitts and Nevis" ' + ((currentVal == "St. Kitts and Nevis")?'selected="true"':'') + '> St. Kitts and Nevis</option>');
	document.write('<option value="Rwanda" ' + ((currentVal == "Rwanda")?'selected="true"':'') + '> Rwanda</option>');
	document.write('<option value="San Marino" ' + ((currentVal == "San Marino")?'selected="true"':'') + '> San Marino</option>');
	document.write('<option value="Sao Tome Principe" ' + ((currentVal == "Sao Tome Principe")?'selected="true"':'') + '> Sao Tome Principe</option>');
	document.write('<option value="Saudi Arabia" ' + ((currentVal == "Saudi Arabia")?'selected="true"':'') + '> Saudi Arabia</option>');
	document.write('<option value="Scotland" ' + ((currentVal == "Scotland")?'selected="true"':'') + '> Scotland</option>');
	document.write('<option value="Senegal" ' + ((currentVal == "Senegal")?'selected="true"':'') + '> Senegal</option>');
	document.write('<option value="Seychelles" ' + ((currentVal == "Seychelles")?'selected="true"':'') + '> Seychelles</option>');
	document.write('<option value="Sierra Leone" ' + ((currentVal == "Sierra Leone")?'selected="true"':'') + '> Sierra Leone</option>');
	document.write('<option value="Slovakia" ' + ((currentVal == "Slovakia")?'selected="true"':'') + '> Slovakia</option>');
	document.write('<option value="Slovenia" ' + ((currentVal == "Slovenia")?'selected="true"':'') + '> Slovenia</option>');
	document.write('<option value="Singapore" ' + ((currentVal == "Singapore")?'selected="true"':'') + '> Singapore</option>');
	document.write('<option value="Solomon Islands" ' + ((currentVal == "Solomon Islands")?'selected="true"':'') + '> Solomon Islands</option>');
	document.write('<option value="Somalia" ' + ((currentVal == "Somalia")?'selected="true"':'') + '> Somalia</option>');
	document.write('<option value="South Africa" ' + ((currentVal == "South Africa")?'selected="true"':'') + '> South Africa</option>');
	document.write('<option value="Spain" ' + ((currentVal == "Spain")?'selected="true"':'') + '> Spain</option>');
	document.write('<option value="Sri Lanka" ' + ((currentVal == "Sri Lanka")?'selected="true"':'') + '> Sri Lanka</option>');
	document.write('<option value="St. Lucia" ' + ((currentVal == "St. Lucia")?'selected="true"':'') + '> St. Lucia</option>');
	document.write('<option value="St. Vincent & Grenadines" ' + ((currentVal == "St. Vincent & Grenadines")?'selected="true"':'') + '> St. Vincent & Grenadines</option>');
	document.write('<option value="Sudan" ' + ((currentVal == "Sudan")?'selected="true"':'') + '> Sudan</option>');
	document.write('<option value="Suriname" ' + ((currentVal == "Suriname")?'selected="true"':'') + '> Suriname</option>');
	document.write('<option value="Swaziland" ' + ((currentVal == "Swaziland")?'selected="true"':'') + '> Swaziland</option>');
	document.write('<option value="Sweden" ' + ((currentVal == "Sweden")?'selected="true"':'') + '> Sweden</option>');
	document.write('<option value="Switzerland" ' + ((currentVal == "Switzerland")?'selected="true"':'') + '> Switzerland</option>');
	document.write('<option value="Syria" ' + ((currentVal == "Syria")?'selected="true"':'') + '> Syria</option>');
	document.write('<option value="Tahiti" ' + ((currentVal == "Tahiti")?'selected="true"':'') + '> Tahiti</option>');
	document.write('<option value="Taiwan" ' + ((currentVal == "Taiwan")?'selected="true"':'') + '> Taiwan</option>');
	document.write('<option value="Tajikstan" ' + ((currentVal == "Tajikstan")?'selected="true"':'') + '> Tajikstan</option>');
	document.write('<option value="Tanzania" ' + ((currentVal == "Tanzania")?'selected="true"':'') + '> Tanzania</option>');
	document.write('<option value="Thailand" ' + ((currentVal == "Thailand")?'selected="true"':'') + '> Thailand</option>');
	document.write('<option value="Togo" ' + ((currentVal == "Togo")?'selected="true"':'') + '> Togo</option>');
	document.write('<option value="Tonga Islands" ' + ((currentVal == "Tonga Islands")?'selected="true"':'') + '> Tonga Islands</option>');
	document.write('<option value="Trinidad and Tobago" ' + ((currentVal == "Trinidad and Tobago")?'selected="true"':'') + '> Trinidad and Tobago</option>');
	document.write('<option value="Tunisia" ' + ((currentVal == "Tunisia")?'selected="true"':'') + '> Tunisia</option>');
	document.write('<option value="Turkmenistan" ' + ((currentVal == "Turkmenistan")?'selected="true"':'') + '> Turkmenistan</option>');
	document.write('<option value="Turkey" ' + ((currentVal == "Turkey")?'selected="true"':'') + '> Turkey</option>');
	document.write('<option value="Turks and Caicos Islands" ' + ((currentVal == "Turks and Caicos Islands")?'selected="true"':'') + '> Turks and Caicos Islands</option>');
	document.write('<option value="Tuvalu" ' + ((currentVal == "Tuvalu")?'selected="true"':'') + '> Tuvalu</option>');
	document.write('<option value="United Kingdom" ' + ((currentVal == "United Kingdom")?'selected="true"':'') + '> United Kingdom</option>');
	document.write('<option value="Ukraine" ' + ((currentVal == "Ukraine")?'selected="true"':'') + '> Ukraine</option>');
	document.write('<option value="Uganda" ' + ((currentVal == "Uganda")?'selected="true"':'') + '> Uganda</option>');
	document.write('<option value="United Arab Emirates" ' + ((currentVal == "United Arab Emirates")?'selected="true"':'') + '> United Arab Emirates</option>');
	document.write('<option value="United States Virgin Islands" ' + ((currentVal == "United States Virgin Islands")?'selected="true"':'') + '> United States Virgin Islands</option>');
	document.write('<option value="Burkina Faso" ' + ((currentVal == "Burkina Faso")?'selected="true"':'') + '> Burkina Faso</option>');
	document.write('<option value="Uzbekistan" ' + ((currentVal == "Uzbekistan")?'selected="true"':'') + '> Uzbekistan</option>');
	document.write('<option value="Uruguay" ' + ((currentVal == "Uruguay")?'selected="true"':'') + '> Uruguay</option>');
	document.write('<option value="Vanuatu" ' + ((currentVal == "Vanuatu")?'selected="true"':'') + '> Vanuatu</option>');
	document.write('<option value="Vatican City" ' + ((currentVal == "Vatican City")?'selected="true"':'') + '> Vatican City</option>');
	document.write('<option value="Venezuela" ' + ((currentVal == "Venezuela")?'selected="true"':'') + '> Venezuela</option>');
	document.write('<option value="Vietnam" ' + ((currentVal == "Vietnam")?'selected="true"':'') + '> Vietnam</option>');
	document.write('<option value="Wales" ' + ((currentVal == "Wales")?'selected="true"':'') + '> Wales</option>');
	document.write('<option value="West Bank" ' + ((currentVal == "West Bank")?'selected="true"':'') + '> West Bank</option>');
	document.write('<option value="West Indies Assoc. States" ' + ((currentVal == "West Indies Assoc. States")?'selected="true"':'') + '> West Indies Assoc. States</option>');
	document.write('<option value="Western Samoa" ' + ((currentVal == "Western Samoa")?'selected="true"':'') + '> Western Samoa</option>');
	document.write('<option value="Yemen" ' + ((currentVal == "Yemen")?'selected="true"':'') + '> Yemen</option>');
	document.write('<option value="Yugoslavia Fed. Rep. Of" ' + ((currentVal == "Yugoslavia Fed. Rep. Of")?'selected="true"':'') + '> Yugoslavia Fed. Rep. Of</option>');
	document.write('<option value="Yukon Territory Canada" ' + ((currentVal == "Yukon Territory Canada")?'selected="true"':'') + '> Yukon Territory Canada</option>');
	document.write('<option value="Zaire" ' + ((currentVal == "Zaire")?'selected="true"':'') + '> Zaire</option>');
	document.write('<option value="Zambia" ' + ((currentVal == "Zambia")?'selected="true"':'') + '> Zambia</option>');
	document.write('<option value="Newfoundland Canada" ' + ((currentVal == "Newfoundland Canada")?'selected="true"':'') + '> Newfoundland Canada</option>');
	document.write('<option value="Northwest Territory Canada" ' + ((currentVal == "Northwest Territory Canada")?'selected="true"':'') + '> Northwest Territory Canada</option>');
	document.write('<option value="Nova Scotia Canada" ' + ((currentVal == "Nova Scotia Canada")?'selected="true"':'') + '> Nova Scotia Canada</option>');
	document.write('<option value="Ontario Canada" ' + ((currentVal == "Ontario Canada")?'selected="true"':'') + '> Ontario Canada</option>');
	document.write('<option value="Prince Edward Isld Canada" ' + ((currentVal == "Prince Edward Isld Canada")?'selected="true"':'') + '> Prince Edward Isld Canada</option>');
	document.write('<option value="Quebec Canada" ' + ((currentVal == "Quebec Canada")?'selected="true"':'') + '> Quebec Canada</option>');
	document.write('<option value="Saskatchewan Canada" ' + ((currentVal == "Saskatchewan Canada")?'selected="true"':'') + '> Saskatchewan Canada</option>');
	document.write('<option value="Alberta Canada" ' + ((currentVal == "Alberta Canada")?'selected="true"':'') + '> Alberta Canada</option>');
	document.write('<option value="British Columbia Canada" ' + ((currentVal == "British Columbia Canada")?'selected="true"':'') + '> British Columbia Canada</option>');
	document.write('<option value="Manitoba Canada" ' + ((currentVal == "Manitoba Canada")?'selected="true"':'') + '> Manitoba Canada</option>');
	document.write('<option value="New Brunswick Canada" ' + ((currentVal == "New Brunswick Canada")?'selected="true"':'') + '> New Brunswick Canada</option>');
	document.write('</select>');
}

function writeStatesSelect(fieldName, currentVal) {	
	document.write('<select name="'+fieldName+'" id="'+fieldName+'" size="1" style="width:150px;">');
	document.write('<option value="">Select Below');
	document.write('<option value="AL" ' + ((currentVal == "AL")?'selected="true"':'') + '> Alabama</option>');
	document.write('<option value="AK" ' + ((currentVal == "AK")?'selected="true"':'') + '> Alaska</option>');
	document.write('<option value="AZ" ' + ((currentVal == "AZ")?'selected="true"':'') + '> Arizona</option>');
	document.write('<option value="AR" ' + ((currentVal == "AR")?'selected="true"':'') + '> Arkansas</option>');
	document.write('<option value="CA" ' + ((currentVal == "CA")?'selected="true"':'') + '> California</option>');
	document.write('<option value="CO" ' + ((currentVal == "CO")?'selected="true"':'') + '> Colorado</option>');
	document.write('<option value="CT" ' + ((currentVal == "CT")?'selected="true"':'') + '> Connecticut</option>');
	document.write('<option value="DE" ' + ((currentVal == "DE")?'selected="true"':'') + '> Delaware</option>');
	document.write('<option value="DC" ' + ((currentVal == "DC")?'selected="true"':'') + '> District of Columbia</option>');
	document.write('<option value="FL" ' + ((currentVal == "FL")?'selected="true"':'') + '> Florida</option>');
	document.write('<option value="GA" ' + ((currentVal == "GA")?'selected="true"':'') + '> Georgia</option>');
	document.write('<option value="HI" ' + ((currentVal == "HI")?'selected="true"':'') + '> Hawaii</option>');
	document.write('<option value="ID" ' + ((currentVal == "ID")?'selected="true"':'') + '> Idaho</option>');
	document.write('<option value="IL" ' + ((currentVal == "IL")?'selected="true"':'') + '> Illinois</option>');
    document.write('<option value="IN" ' + ((currentVal == "IN")?'selected="true"':'') + '> Indiana</option>');
    document.write('<option value="IA" ' + ((currentVal == "IA")?'selected="true"':'') + '> Iowa</option>');
    document.write('<option value="KS" ' + ((currentVal == "KS")?'selected="true"':'') + '> Kansas</option>');
    document.write('<option value="KY" ' + ((currentVal == "KY")?'selected="true"':'') + '> Kentucky</option>');
    document.write('<option value="LA" ' + ((currentVal == "LA")?'selected="true"':'') + '> Louisiana</option>');
    document.write('<option value="ME" ' + ((currentVal == "ME")?'selected="true"':'') + '> Maine</option>');
    document.write('<option value="MD" ' + ((currentVal == "MD")?'selected="true"':'') + '> Maryland</option>');
    document.write('<option value="MA" ' + ((currentVal == "MA")?'selected="true"':'') + '> Massachusetts</option>');
    document.write('<option value="MI" ' + ((currentVal == "MI")?'selected="true"':'') + '> Michigan</option>');
    document.write('<option value="MN" ' + ((currentVal == "MN")?'selected="true"':'') + '> Minnesota</option>');
    document.write('<option value="MS" ' + ((currentVal == "MS")?'selected="true"':'') + '> Mississippi</option>');
    document.write('<option value="MO" ' + ((currentVal == "MO")?'selected="true"':'') + '> Missouri</option>');
    document.write('<option value="MT" ' + ((currentVal == "MT")?'selected="true"':'') + '> Montana</option>');
    document.write('<option value="NE" ' + ((currentVal == "NE")?'selected="true"':'') + '> Nebraska</option>');
    document.write('<option value="NV" ' + ((currentVal == "NV")?'selected="true"':'') + '> Nevada</option>');
    document.write('<option value="NH" ' + ((currentVal == "NH")?'selected="true"':'') + '> New Hampshire</option>');
    document.write('<option value="NJ" ' + ((currentVal == "NJ")?'selected="true"':'') + '> New Jersey</option>');
    document.write('<option value="NM" ' + ((currentVal == "NM")?'selected="true"':'') + '> New Mexico</option>');
    document.write('<option value="NY" ' + ((currentVal == "NY")?'selected="true"':'') + '> New York</option>');
    document.write('<option value="NC" ' + ((currentVal == "NC")?'selected="true"':'') + '> North Carolina</option>');
    document.write('<option value="ND" ' + ((currentVal == "ND")?'selected="true"':'') + '> North Dakota</option>');
    document.write('<option value="OH" ' + ((currentVal == "OH")?'selected="true"':'') + '> Ohio</option>');
    document.write('<option value="OK" ' + ((currentVal == "OK")?'selected="true"':'') + '> Oklahoma</option>');
    document.write('<option value="OR" ' + ((currentVal == "OR")?'selected="true"':'') + '> Oregon</option>');
    document.write('<option value="PA" ' + ((currentVal == "PA")?'selected="true"':'') + '> Pennsylvania</option>');
    document.write('<option value="RI" ' + ((currentVal == "RI")?'selected="true"':'') + '> Rhode Island</option>');
    document.write('<option value="SC" ' + ((currentVal == "SC")?'selected="true"':'') + '> South Carolina</option>');
    document.write('<option value="SD" ' + ((currentVal == "SD")?'selected="true"':'') + '> South Dakota</option>');
    document.write('<option value="TN" ' + ((currentVal == "TN")?'selected="true"':'') + '> Tennessee</option>');
    document.write('<option value="TX" ' + ((currentVal == "TX")?'selected="true"':'') + '> Texas</option>');
    document.write('<option value="UT" ' + ((currentVal == "UT")?'selected="true"':'') + '> Utah</option>');
    document.write('<option value="VT" ' + ((currentVal == "VT")?'selected="true"':'') + '> Vermont</option>');
    document.write('<option value="VA" ' + ((currentVal == "VA")?'selected="true"':'') + '> Virginia</option>');
    document.write('<option value="WA" ' + ((currentVal == "WA")?'selected="true"':'') + '> Washington</option>');
    document.write('<option value="WV" ' + ((currentVal == "WV")?'selected="true"':'') + '> West Virginia</option>');
    document.write('<option value="WI" ' + ((currentVal == "WI")?'selected="true"':'') + '> Wisconsin</option>');
    document.write('<option value="WY" ' + ((currentVal == "WY")?'selected="true"':'') + '> Wyoming</option>');
    document.write('<option value="otherCountry" ' + ((currentVal == "otherCountry")?'selected="true"':'') + '> None - Outside of USA</option>');
	document.write('</select>');
}

function writeStatesOptions(currentVal) {	
	document.write('<option value="">Select Below');
	document.write('<option value="AL" ' + ((currentVal == "AL")?'selected="true"':'') + '> Alabama</option>');
	document.write('<option value="AK" ' + ((currentVal == "AK")?'selected="true"':'') + '> Alaska</option>');
	document.write('<option value="AZ" ' + ((currentVal == "AZ")?'selected="true"':'') + '> Arizona</option>');
	document.write('<option value="AR" ' + ((currentVal == "AR")?'selected="true"':'') + '> Arkansas</option>');
	document.write('<option value="CA" ' + ((currentVal == "CA")?'selected="true"':'') + '> California</option>');
	document.write('<option value="CO" ' + ((currentVal == "CO")?'selected="true"':'') + '> Colorado</option>');
	document.write('<option value="CT" ' + ((currentVal == "CT")?'selected="true"':'') + '> Connecticut</option>');
	document.write('<option value="DE" ' + ((currentVal == "DE")?'selected="true"':'') + '> Delaware</option>');
	document.write('<option value="DC" ' + ((currentVal == "DC")?'selected="true"':'') + '> District of Columbia</option>');
	document.write('<option value="FL" ' + ((currentVal == "FL")?'selected="true"':'') + '> Florida</option>');
	document.write('<option value="GA" ' + ((currentVal == "GA")?'selected="true"':'') + '> Georgia</option>');
	document.write('<option value="HI" ' + ((currentVal == "HI")?'selected="true"':'') + '> Hawaii</option>');
	document.write('<option value="ID" ' + ((currentVal == "ID")?'selected="true"':'') + '> Idaho</option>');
	document.write('<option value="IL" ' + ((currentVal == "IL")?'selected="true"':'') + '> Illinois</option>');
    document.write('<option value="IN" ' + ((currentVal == "IN")?'selected="true"':'') + '> Indiana</option>');
    document.write('<option value="IA" ' + ((currentVal == "IA")?'selected="true"':'') + '> Iowa</option>');
    document.write('<option value="KS" ' + ((currentVal == "KS")?'selected="true"':'') + '> Kansas</option>');
    document.write('<option value="KY" ' + ((currentVal == "KY")?'selected="true"':'') + '> Kentucky</option>');
    document.write('<option value="LA" ' + ((currentVal == "LA")?'selected="true"':'') + '> Louisiana</option>');
    document.write('<option value="ME" ' + ((currentVal == "ME")?'selected="true"':'') + '> Maine</option>');
    document.write('<option value="MD" ' + ((currentVal == "MD")?'selected="true"':'') + '> Maryland</option>');
    document.write('<option value="MA" ' + ((currentVal == "MA")?'selected="true"':'') + '> Massachusetts</option>');
    document.write('<option value="MI" ' + ((currentVal == "MI")?'selected="true"':'') + '> Michigan</option>');
    document.write('<option value="MN" ' + ((currentVal == "MN")?'selected="true"':'') + '> Minnesota</option>');
    document.write('<option value="MS" ' + ((currentVal == "MS")?'selected="true"':'') + '> Mississippi</option>');
    document.write('<option value="MO" ' + ((currentVal == "MO")?'selected="true"':'') + '> Missouri</option>');
    document.write('<option value="MT" ' + ((currentVal == "MT")?'selected="true"':'') + '> Montana</option>');
    document.write('<option value="NE" ' + ((currentVal == "NE")?'selected="true"':'') + '> Nebraska</option>');
    document.write('<option value="NV" ' + ((currentVal == "NV")?'selected="true"':'') + '> Nevada</option>');
    document.write('<option value="NH" ' + ((currentVal == "NH")?'selected="true"':'') + '> New Hampshire</option>');
    document.write('<option value="NJ" ' + ((currentVal == "NJ")?'selected="true"':'') + '> New Jersey</option>');
    document.write('<option value="NM" ' + ((currentVal == "NM")?'selected="true"':'') + '> New Mexico</option>');
    document.write('<option value="NY" ' + ((currentVal == "NY")?'selected="true"':'') + '> New York</option>');
    document.write('<option value="NC" ' + ((currentVal == "NC")?'selected="true"':'') + '> North Carolina</option>');
    document.write('<option value="ND" ' + ((currentVal == "ND")?'selected="true"':'') + '> North Dakota</option>');
    document.write('<option value="OH" ' + ((currentVal == "OH")?'selected="true"':'') + '> Ohio</option>');
    document.write('<option value="OK" ' + ((currentVal == "OK")?'selected="true"':'') + '> Oklahoma</option>');
    document.write('<option value="OR" ' + ((currentVal == "OR")?'selected="true"':'') + '> Oregon</option>');
    document.write('<option value="PA" ' + ((currentVal == "PA")?'selected="true"':'') + '> Pennsylvania</option>');
    document.write('<option value="RI" ' + ((currentVal == "RI")?'selected="true"':'') + '> Rhode Island</option>');
    document.write('<option value="SC" ' + ((currentVal == "SC")?'selected="true"':'') + '> South Carolina</option>');
    document.write('<option value="SD" ' + ((currentVal == "SD")?'selected="true"':'') + '> South Dakota</option>');
    document.write('<option value="TN" ' + ((currentVal == "TN")?'selected="true"':'') + '> Tennessee</option>');
    document.write('<option value="TX" ' + ((currentVal == "TX")?'selected="true"':'') + '> Texas</option>');
    document.write('<option value="UT" ' + ((currentVal == "UT")?'selected="true"':'') + '> Utah</option>');
    document.write('<option value="VT" ' + ((currentVal == "VT")?'selected="true"':'') + '> Vermont</option>');
    document.write('<option value="VA" ' + ((currentVal == "VA")?'selected="true"':'') + '> Virginia</option>');
    document.write('<option value="WA" ' + ((currentVal == "WA")?'selected="true"':'') + '> Washington</option>');
    document.write('<option value="WV" ' + ((currentVal == "WV")?'selected="true"':'') + '> West Virginia</option>');
    document.write('<option value="WI" ' + ((currentVal == "WI")?'selected="true"':'') + '> Wisconsin</option>');
    document.write('<option value="WY" ' + ((currentVal == "WY")?'selected="true"':'') + '> Wyoming</option>');
    document.write('<option value="otherCountry" ' + ((currentVal == "otherCountry")?'selected="true"':'') + '> None - Outside of USA</option>');
}

function writeStatesOptionsUnknownOtherStates(currentVal) {	
	document.write('<option value="">Select Below');
	document.write('<option value="AL" ' + ((currentVal == "AL")?'selected="true"':'') + '> Alabama</option>');
	document.write('<option value="AK" ' + ((currentVal == "AK")?'selected="true"':'') + '> Alaska</option>');
	document.write('<option value="AZ" ' + ((currentVal == "AZ")?'selected="true"':'') + '> Arizona</option>');
	document.write('<option value="AR" ' + ((currentVal == "AR")?'selected="true"':'') + '> Arkansas</option>');
	document.write('<option value="CA" ' + ((currentVal == "CA")?'selected="true"':'') + '> California</option>');
	document.write('<option value="CO" ' + ((currentVal == "CO")?'selected="true"':'') + '> Colorado</option>');
	document.write('<option value="CT" ' + ((currentVal == "CT")?'selected="true"':'') + '> Connecticut</option>');
	document.write('<option value="DE" ' + ((currentVal == "DE")?'selected="true"':'') + '> Delaware</option>');
	document.write('<option value="DC" ' + ((currentVal == "DC")?'selected="true"':'') + '> District of Columbia</option>');
	document.write('<option value="FL" ' + ((currentVal == "FL")?'selected="true"':'') + '> Florida</option>');
	document.write('<option value="GA" ' + ((currentVal == "GA")?'selected="true"':'') + '> Georgia</option>');
	document.write('<option value="HI" ' + ((currentVal == "HI")?'selected="true"':'') + '> Hawaii</option>');
	document.write('<option value="ID" ' + ((currentVal == "ID")?'selected="true"':'') + '> Idaho</option>');
	document.write('<option value="IL" ' + ((currentVal == "IL")?'selected="true"':'') + '> Illinois</option>');
    document.write('<option value="IN" ' + ((currentVal == "IN")?'selected="true"':'') + '> Indiana</option>');
    document.write('<option value="IA" ' + ((currentVal == "IA")?'selected="true"':'') + '> Iowa</option>');
    document.write('<option value="KS" ' + ((currentVal == "KS")?'selected="true"':'') + '> Kansas</option>');
    document.write('<option value="KY" ' + ((currentVal == "KY")?'selected="true"':'') + '> Kentucky</option>');
    document.write('<option value="LA" ' + ((currentVal == "LA")?'selected="true"':'') + '> Louisiana</option>');
    document.write('<option value="ME" ' + ((currentVal == "ME")?'selected="true"':'') + '> Maine</option>');
    document.write('<option value="MD" ' + ((currentVal == "MD")?'selected="true"':'') + '> Maryland</option>');
    document.write('<option value="MA" ' + ((currentVal == "MA")?'selected="true"':'') + '> Massachusetts</option>');
    document.write('<option value="MI" ' + ((currentVal == "MI")?'selected="true"':'') + '> Michigan</option>');
    document.write('<option value="MN" ' + ((currentVal == "MN")?'selected="true"':'') + '> Minnesota</option>');
    document.write('<option value="MS" ' + ((currentVal == "MS")?'selected="true"':'') + '> Mississippi</option>');
    document.write('<option value="MO" ' + ((currentVal == "MO")?'selected="true"':'') + '> Missouri</option>');
    document.write('<option value="MT" ' + ((currentVal == "MT")?'selected="true"':'') + '> Montana</option>');
    document.write('<option value="NE" ' + ((currentVal == "NE")?'selected="true"':'') + '> Nebraska</option>');
    document.write('<option value="NV" ' + ((currentVal == "NV")?'selected="true"':'') + '> Nevada</option>');
    document.write('<option value="NH" ' + ((currentVal == "NH")?'selected="true"':'') + '> New Hampshire</option>');
    document.write('<option value="NJ" ' + ((currentVal == "NJ")?'selected="true"':'') + '> New Jersey</option>');
    document.write('<option value="NM" ' + ((currentVal == "NM")?'selected="true"':'') + '> New Mexico</option>');
    document.write('<option value="NY" ' + ((currentVal == "NY")?'selected="true"':'') + '> New York</option>');
    document.write('<option value="NC" ' + ((currentVal == "NC")?'selected="true"':'') + '> North Carolina</option>');
    document.write('<option value="ND" ' + ((currentVal == "ND")?'selected="true"':'') + '> North Dakota</option>');
    document.write('<option value="OH" ' + ((currentVal == "OH")?'selected="true"':'') + '> Ohio</option>');
    document.write('<option value="OK" ' + ((currentVal == "OK")?'selected="true"':'') + '> Oklahoma</option>');
    document.write('<option value="OR" ' + ((currentVal == "OR")?'selected="true"':'') + '> Oregon</option>');
    document.write('<option value="PA" ' + ((currentVal == "PA")?'selected="true"':'') + '> Pennsylvania</option>');
    document.write('<option value="RI" ' + ((currentVal == "RI")?'selected="true"':'') + '> Rhode Island</option>');
    document.write('<option value="SC" ' + ((currentVal == "SC")?'selected="true"':'') + '> South Carolina</option>');
    document.write('<option value="SD" ' + ((currentVal == "SD")?'selected="true"':'') + '> South Dakota</option>');
    document.write('<option value="TN" ' + ((currentVal == "TN")?'selected="true"':'') + '> Tennessee</option>');
    document.write('<option value="TX" ' + ((currentVal == "TX")?'selected="true"':'') + '> Texas</option>');
    document.write('<option value="UT" ' + ((currentVal == "UT")?'selected="true"':'') + '> Utah</option>');
    document.write('<option value="VT" ' + ((currentVal == "VT")?'selected="true"':'') + '> Vermont</option>');
    document.write('<option value="VA" ' + ((currentVal == "VA")?'selected="true"':'') + '> Virginia</option>');
    document.write('<option value="WA" ' + ((currentVal == "WA")?'selected="true"':'') + '> Washington</option>');
    document.write('<option value="WV" ' + ((currentVal == "WV")?'selected="true"':'') + '> West Virginia</option>');
    document.write('<option value="WI" ' + ((currentVal == "WI")?'selected="true"':'') + '> Wisconsin</option>');
    document.write('<option value="WY" ' + ((currentVal == "WY")?'selected="true"':'') + '> Wyoming</option>');
    document.write('<option value="otherCountry" ' + ((currentVal == "otherCountry")?'selected="true"':'') + '> None - Outside of USA</option>');
    
}

function writeStatesName(currentVal) 
{
	var state = "";
	if (currentVal == "AL")
	{
		state = "Alabama";
	}
	else if (currentVal == "AK")
	{
		state = "Alaska";
	}
	else if (currentVal == "AZ")
	{
		state = "Arizona";
	}
	else if (currentVal == "AR")
	{
		state = "Arkansas";
	}
	else if (currentVal == "CA")
	{
		state = "California";
	}
	else if (currentVal == "CO")
	{
		state = "Colorado";
	}
	else if (currentVal == "CT")
	{
		state = "Connecticut";
	}
	else if (currentVal == "DE")
	{
		state = "Delaware";
	}
	else if (currentVal == "DC")
	{
		state = "District of Columbia";
	}
	else if (currentVal == "FL")
	{
		state = "Florida";
	}
	else if (currentVal == "GA")
	{
		state = "Georgia";
	}
	else if (currentVal == "HI")
	{
		state = "Hawaii";
	}
	else if (currentVal == "ID")
	{
		state = "Idaho";
	}
	else if (currentVal == "IL")
	{
		state = "Illinois";
	}
		else if (currentVal == "IN")
	{
		state = "Indiana";
	}

	else if (currentVal == "IA")
	{
		state = "Iowa";
	}

	else if (currentVal == "KS")
	{
		state = "Kansas";
	}
	else if (currentVal == "KY")
	{
		state = "Kentucky";
	}
	else if (currentVal == "LA")
	{
		state = "Louisiana";
	}
	else if (currentVal == "ME")
	{
		state = "Maine";
	}
	else if (currentVal == "MD")
	{
		state = "Maryland";
	}
	else if (currentVal == "MA")
	{
		state = "Massachusetts";
	}
		else if (currentVal == "MI")
	{
		state = "Michigan";
	}
		else if (currentVal == "MN")
	{
		state = "Minnesota";
	}
	else if (currentVal == "MS")
	{
		state = "Mississippi";
	}
	else if (currentVal == "MO")
	{
		state = "Missouri";
	}
	else if (currentVal == "MT")
	{
		state = "Montana";
	}
	else if (currentVal == "NE")
	{
		state = "Nebraska";
	}
	else if (currentVal == "NV")
	{
		state = "Nevada";
	}
	else if (currentVal == "NH")
	{
		state = "New Hampshire";
	}
	else if (currentVal == "NJ")
	{
		state = "New Jersey";
	}
	else if (currentVal == "NM")
	{
		state = "New Mexico";
	}
	else if (currentVal == "NY")
	{
		state = "New York";
	}
	else if (currentVal == "NC")
	{
		state = "North Carolina";
	}
	else if (currentVal == "ND")
	{
		state = "North Dakota";
	}
	else if (currentVal == "OH")
	{
		state = "Ohio";
	}
	else if (currentVal == "OK")
	{
		state = "Oklahoma";
	}
	else if (currentVal == "OR")
	{
		state = "Oregon";
	}
	else if (currentVal == "PA")
	{
		state = "Pennsylvania";
	}
	else if (currentVal == "RI")
	{
		state = "Rhode Island";
	}
	else if (currentVal == "SC")
	{
		state = "South Carolina";
	}
	else if (currentVal == "SD")
	{
		state = "South Dakota";
	}
	else if (currentVal == "TN")
	{
		state = "Tennessee";
	}
	else if (currentVal == "TX")
	{
		state = "Texas";
	}
	else if (currentVal == "UT")
	{
		state = "Utah";
	}
	else if (currentVal == "VT")
	{
		state = "Vermont";
	}
	else if (currentVal == "VA")
	{
		state = "Virginia";
	}
	else if (currentVal == "WA")
	{
		state = "Washington";
	}
	else if (currentVal == "WV")
	{
		state = "West Virginia";
	}
	else if (currentVal == "WI")
	{
		state = "Wisconsin";
	}
	else if (currentVal == "WY")
	{
		state = "Wyoming";
	}
	else if (currentVal == "otherCountry")
	{
		state = "None - Outside of USA";
	}
	else
	{
		state = "None - Outside of USA";
	}
	return state;
}
function writeShortStatesSelect(fieldName, currentVal) {	
	document.write('<select style="width:55px" name="'+fieldName+'" id="'+fieldName+'">');
	document.write('<option value="">N/A');
	document.write('<option value="AL" ' + ((currentVal == "AL")?'selected="true"':'') + '> AL</option>');
	document.write('<option value="AK" ' + ((currentVal == "AK")?'selected="true"':'') + '> AK</option>');
	document.write('<option value="AZ" ' + ((currentVal == "AZ")?'selected="true"':'') + '> AZ</option>');
	document.write('<option value="AR" ' + ((currentVal == "AR")?'selected="true"':'') + '> AR</option>');
	document.write('<option value="CA" ' + ((currentVal == "CA")?'selected="true"':'') + '> CA</option>');
	document.write('<option value="CO" ' + ((currentVal == "CO")?'selected="true"':'') + '> CO</option>');
	document.write('<option value="CT" ' + ((currentVal == "CT")?'selected="true"':'') + '> CT</option>');
	document.write('<option value="DE" ' + ((currentVal == "DE")?'selected="true"':'') + '> DE</option>');
	document.write('<option value="DC" ' + ((currentVal == "DC")?'selected="true"':'') + '> DC</option>');
	document.write('<option value="FL" ' + ((currentVal == "FL")?'selected="true"':'') + '> FL</option>');
	document.write('<option value="GA" ' + ((currentVal == "GA")?'selected="true"':'') + '> GA</option>');
	document.write('<option value="HI" ' + ((currentVal == "HI")?'selected="true"':'') + '> HI</option>');
	document.write('<option value="ID" ' + ((currentVal == "ID")?'selected="true"':'') + '> ID</option>');
	document.write('<option value="IL" ' + ((currentVal == "IL")?'selected="true"':'') + '> IL</option>');
    document.write('<option value="IN" ' + ((currentVal == "IN")?'selected="true"':'') + '> IN</option>');
    document.write('<option value="IA" ' + ((currentVal == "IA")?'selected="true"':'') + '> IA</option>');
    document.write('<option value="KS" ' + ((currentVal == "KS")?'selected="true"':'') + '> KS</option>');
    document.write('<option value="KY" ' + ((currentVal == "KY")?'selected="true"':'') + '> KY</option>');
    document.write('<option value="LA" ' + ((currentVal == "LA")?'selected="true"':'') + '> LA</option>');
    document.write('<option value="ME" ' + ((currentVal == "ME")?'selected="true"':'') + '> ME</option>');
    document.write('<option value="MD" ' + ((currentVal == "MD")?'selected="true"':'') + '> MD</option>');
    document.write('<option value="MA" ' + ((currentVal == "MA")?'selected="true"':'') + '> MA</option>');
    document.write('<option value="MI" ' + ((currentVal == "MI")?'selected="true"':'') + '> MI</option>');
    document.write('<option value="MN" ' + ((currentVal == "MN")?'selected="true"':'') + '> MN</option>');
    document.write('<option value="MS" ' + ((currentVal == "MS")?'selected="true"':'') + '> MS</option>');
    document.write('<option value="MO" ' + ((currentVal == "MO")?'selected="true"':'') + '> MO</option>');
    document.write('<option value="MT" ' + ((currentVal == "MT")?'selected="true"':'') + '> MT</option>');
    document.write('<option value="NE" ' + ((currentVal == "NE")?'selected="true"':'') + '> NE</option>');
    document.write('<option value="NV" ' + ((currentVal == "NV")?'selected="true"':'') + '> NV</option>');
    document.write('<option value="NH" ' + ((currentVal == "NH")?'selected="true"':'') + '> NH</option>');
    document.write('<option value="NJ" ' + ((currentVal == "NJ")?'selected="true"':'') + '> NJ</option>');
    document.write('<option value="NM" ' + ((currentVal == "NM")?'selected="true"':'') + '> NM</option>');
    document.write('<option value="NY" ' + ((currentVal == "NY")?'selected="true"':'') + '> NY</option>');
    document.write('<option value="NC" ' + ((currentVal == "NC")?'selected="true"':'') + '> NC</option>');
    document.write('<option value="ND" ' + ((currentVal == "ND")?'selected="true"':'') + '> ND</option>');
    document.write('<option value="OH" ' + ((currentVal == "OH")?'selected="true"':'') + '> OH</option>');
    document.write('<option value="OK" ' + ((currentVal == "OK")?'selected="true"':'') + '> OK</option>');
    document.write('<option value="OR" ' + ((currentVal == "OR")?'selected="true"':'') + '> OR</option>');
    document.write('<option value="PA" ' + ((currentVal == "PA")?'selected="true"':'') + '> PA</option>');
    document.write('<option value="RI" ' + ((currentVal == "RI")?'selected="true"':'') + '> RI</option>');
    document.write('<option value="SC" ' + ((currentVal == "SC")?'selected="true"':'') + '> SC</option>');
    document.write('<option value="SD" ' + ((currentVal == "SD")?'selected="true"':'') + '> SD</option>');
    document.write('<option value="TN" ' + ((currentVal == "TN")?'selected="true"':'') + '> TN</option>');
    document.write('<option value="TX" ' + ((currentVal == "TX")?'selected="true"':'') + '> TX</option>');
    document.write('<option value="UT" ' + ((currentVal == "UT")?'selected="true"':'') + '> UT</option>');
    document.write('<option value="VT" ' + ((currentVal == "VT")?'selected="true"':'') + '> VT</option>');
    document.write('<option value="VA" ' + ((currentVal == "VA")?'selected="true"':'') + '> VA</option>');
    document.write('<option value="WA" ' + ((currentVal == "WA")?'selected="true"':'') + '> WA</option>');
    document.write('<option value="WV" ' + ((currentVal == "WV")?'selected="true"':'') + '> WV</option>');
    document.write('<option value="WI" ' + ((currentVal == "WI")?'selected="true"':'') + '> WI</option>');
    document.write('<option value="WY" ' + ((currentVal == "WY")?'selected="true"':'') + '> WY</option>');
	document.write('</select>');
}