/// <reference path="../../../../../typings/angular2/angular2.d.ts" />

import {Directive, LifecycleEvent, Attribute, Host, SkipSelf, EventEmitter, NgFor, NgIf, Component, View} from 'angular2/angular2';

@Component({
  selector: 'conditionlet users-country-conditionlet'
})
@View({
  directives: [NgFor],
  template: `
    <div class="col-sm-5">
      <select class="form-control clause-selector" [value]="conditionletDir.condition.comparison" (change)="setComparison($event)">
        <option value="{{x.id}}"
                *ng-for="var x of conditionletDir.conditionlet.comparisons"
                [selected]="x.id == conditionletDir.condition.comparison">{{x.label}}</option>
      </select>
    </div>
    <div class="col-sm-2">
      <h4 class="separator"></h4>
    </div>
    <div class="col-sm-5">
      <select class="form-control clause-selector" [value]="conditionletDir.value" (change)="setValue($event)">
        <option value="{{country.id}}"
                *ng-for="var country of countries"
                [selected]="country.id == conditionletDir.value">{{country.label}}</option>
      </select>
    </div>
  `
})
export class UsersCountryConditionlet {
  countries:any

  constructor(@Attribute('id') id:string) {

    //TODO: We need to update this to hit and endpoint after discuss i18n.
    this.countries = [
      {
        "id": "AF",
        "label": "Afganistan"
      },
      {
        "id": "AL",
        "label": "Albania"
      },
      {
        "id": "DZ",
        "label": "Algeria"
      },
      {
        "id": "AS",
        "label": "American Samoa"
      },
      {
        "id": "AD",
        "label": "Andorra"
      },
      {
        "id": "AO",
        "label": "Angola"
      },
      {
        "id": "AI",
        "label": "Anguilla"
      },
      {
        "id": "AQ",
        "label": "Antarctica"
      },
      {
        "id": "AR",
        "label": "Argentina"
      },
      {
        "id": "AM",
        "label": "Armenia"
      },
      {
        "id": "AW",
        "label": "Aruba"
      },
      {
        "id": "AU",
        "label": "Australia"
      },
      {
        "id": "AT",
        "label": "Austria"
      },
      {
        "id": "AZ",
        "label": "Azerbaijan"
      },
      {
        "id": "BS",
        "label": "Bahamas"
      },
      {
        "id": "BH",
        "label": "Bahrain"
      },
      {
        "id": "BD",
        "label": "Bangladesh"
      },
      {
        "id": "BB",
        "label": "Barbados"
      },
      {
        "id": "BY",
        "label": "Belarus"
      },
      {
        "id": "BE",
        "label": "Belgium"
      },
      {
        "id": "BZ",
        "label": "Belize"
      },
      {
        "id": "BJ",
        "label": "Benin"
      },
      {
        "id": "BM",
        "label": "Bermuda"
      },
      {
        "id": "BT",
        "label": "Bhutan"
      },
      {
        "id": "BO",
        "label": "Bolivia"
      },
      {
        "id": "BA",
        "label": "Bosnia and Herzegovina"
      },
      {
        "id": "BW",
        "label": "Botswana"
      },
      {
        "id": "BR",
        "label": "Brazil"
      },
      {
        "id": "VG",
        "label": "British Virgin Islands"
      },
      {
        "id": "BN",
        "label": "Brunei"
      },
      {
        "id": "BG",
        "label": "Bulgaria"
      },
      {
        "id": "BF",
        "label": "Burkina Faso"
      },
      {
        "id": "BI",
        "label": "Burundi"
      },
      {
        "id": "KH",
        "label": "Cambodia"
      },
      {
        "id": "CM",
        "label": "Cameroon"
      },
      {
        "id": "CA",
        "label": "Canada"
      },
      {
        "id": "CV",
        "label": "Cape Verde"
      },
      {
        "id": "KY",
        "label": "Cayman Islands"
      },
      {
        "id": "CF",
        "label": "Central African Republic"
      },
      {
        "id": "CL",
        "label": "Chile"
      },
      {
        "id": "CN",
        "label": "China"
      },
      {
        "id": "CO",
        "label": "Colombia"
      },
      {
        "id": "KM",
        "label": "Comoros"
      },
      {
        "id": "CK",
        "label": "Cook Islands"
      },
      {
        "id": "CR",
        "label": "Costa Rica"
      },
      {
        "id": "HR",
        "label": "Croatia"
      },
      {
        "id": "CU",
        "label": "Cuba"
      },
      {
        "id": "CW",
        "label": "Curacao"
      },
      {
        "id": "CY",
        "label": "Cyprus"
      },
      {
        "id": "CZ",
        "label": "Czech Republic"
      },
      {
        "id": "CD",
        "label": "Democratic Republic of Congo"
      },
      {
        "id": "DK",
        "label": "Denmark"
      },
      {
        "id": "DJ",
        "label": "Djibouti"
      },
      {
        "id": "DM",
        "label": "Dominica"
      },
      {
        "id": "DO",
        "label": "Dominican Republic"
      },
      {
        "id": "TL",
        "label": "East Timor"
      },
      {
        "id": "EC",
        "label": "Ecuador"
      },
      {
        "id": "EG",
        "label": "Egypt"
      },
      {
        "id": "SV",
        "label": "El Salvador"
      },
      {
        "id": "GQ",
        "label": "Equatorial Guinea"
      },
      {
        "id": "ER",
        "label": "Eritrea"
      },
      {
        "id": "EE",
        "label": "Estonia"
      },
      {
        "id": "ET",
        "label": "Ethiopia"
      },
      {
        "id": "FK",
        "label": "Falkland Islands"
      },
      {
        "id": "FO",
        "label": "Faroe Islands"
      },
      {
        "id": "FJ",
        "label": "Fiji"
      },
      {
        "id": "FI",
        "label": "Finland"
      },
      {
        "id": "FR",
        "label": "France"
      },
      {
        "id": "PF",
        "label": "French Polynesia"
      },
      {
        "id": "GA",
        "label": "Gabon"
      },
      {
        "id": "GM",
        "label": "Gambia"
      },
      {
        "id": "GE",
        "label": "Georgia"
      },
      {
        "id": "DE",
        "label": "Germany"
      },
      {
        "id": "GH",
        "label": "Ghana"
      },
      {
        "id": "GI",
        "label": "Gibraltar"
      },
      {
        "id": "GR",
        "label": "Greece"
      },
      {
        "id": "GL",
        "label": "Greenland"
      },
      {
        "id": "GP",
        "label": "Guadeloupe"
      },
      {
        "id": "GU",
        "label": "Guam"
      },
      {
        "id": "GT",
        "label": "Guatemala"
      },
      {
        "id": "GN",
        "label": "Guinea"
      },
      {
        "id": "GW",
        "label": "Guinea-Bissau"
      },
      {
        "id": "GY",
        "label": "Guyana"
      },
      {
        "id": "HT",
        "label": "Haiti"
      },
      {
        "id": "HN",
        "label": "Honduras"
      },
      {
        "id": "HK",
        "label": "Hong Kong"
      },
      {
        "id": "HU",
        "label": "Hungary"
      },
      {
        "id": "IS",
        "label": "Iceland"
      },
      {
        "id": "IN",
        "label": "India"
      },
      {
        "id": "ID",
        "label": "Indonesia"
      },
      {
        "id": "IR",
        "label": "Iran"
      },
      {
        "id": "IQ",
        "label": "Irak"
      },
      {
        "id": "IE",
        "label": "Ireland"
      },
      {
        "id": "IM",
        "label": "Isle of Man"
      },
      {
        "id": "IL",
        "label": "Israel"
      },
      {
        "id": "IT",
        "label": "Italy"
      },
      {
        "id": "CI",
        "label": "Ivory Coast"
      },
      {
        "id": "JM",
        "label": "Jamaica"
      },
      {
        "id": "JP",
        "label": "Japan"
      },
      {
        "id": "JO",
        "label": "Jordan"
      },
      {
        "id": "KZ",
        "label": "Kazakhstan"
      },
      {
        "id": "KE",
        "label": "Kenya"
      },
      {
        "id": "KI",
        "label": "Kiribati"
      },
      {
        "id": "XK",
        "label": "Kosovo"
      },
      {
        "id": "KW",
        "label": "Kuwait"
      },
      {
        "id": "KG",
        "label": "Kyrgyzstan"
      },
      {
        "id": "LA",
        "label": "Laos"
      },
      {
        "id": "LV",
        "label": "Latvia"
      },
      {
        "id": "LB",
        "label": "Lebanon"
      },
      {
        "id": "LS",
        "label": "Lesotho"
      },
      {
        "id": "LR",
        "label": "Liberia"
      },
      {
        "id": "LY",
        "label": "Libya"
      },
      {
        "id": "LI",
        "label": "Liechtenstein"
      },
      {
        "id": "LT",
        "label": "Lithuania"
      },
      {
        "id": "LU",
        "label": "Luxembourg"
      },
      {
        "id": "MO",
        "label": "Macau"
      },
      {
        "id": "MK",
        "label": "Macedonia"
      },
      {
        "id": "MG",
        "label": "Madagascar"
      },
      {
        "id": "MW",
        "label": "Malawi"
      },
      {
        "id": "MY",
        "label": "Malaysia"
      },
      {
        "id": "MV",
        "label": "Maldives"
      },
      {
        "id": "ML",
        "label": "Mali"
      },
      {
        "id": "MT",
        "label": "Malta"
      },
      {
        "id": "MH",
        "label": "Marshall Islands"
      },
      {
        "id": "MR",
        "label": "Mauritania"
      },
      {
        "id": "MU",
        "label": "Mauritius"
      },
      {
        "id": "MX",
        "label": "Mexico"
      },
      {
        "id": "FM",
        "label": "Micronesia"
      },
      {
        "id": "MD",
        "label": "Moldova"
      },
      {
        "id": "MC",
        "label": "Monaco"
      },
      {
        "id": "MN",
        "label": "Mongolia"
      },
      {
        "id": "ME",
        "label": "Montenegro"
      },
      {
        "id": "MS",
        "label": "Montserrat"
      },
      {
        "id": "MA",
        "label": "Morocco"
      },
      {
        "id": "MZ",
        "label": "Mozambique"
      },
      {
        "id": "MM",
        "label": "Myanmar"
      },
      {
        "id": "NA",
        "label": "Namibia"
      },
      {
        "id": "NR",
        "label": "Nauru"
      },
      {
        "id": "NP",
        "label": "Nepal"
      },
      {
        "id": "NL",
        "label": "Netherlands"
      },
      {
        "id": "NC",
        "label": "New Caledonia"
      },
      {
        "id": "NZ",
        "label": "New Zealand"
      },
      {
        "id": "NI",
        "label": "Nicaragua"
      },
      {
        "id": "NE",
        "label": "Niger"
      },
      {
        "id": "NG",
        "label": "Nigeria"
      },
      {
        "id": "NU",
        "label": "Niue"
      },
      {
        "id": "NF",
        "label": "Norfolk Island"
      },
      {
        "id": "KP",
        "label": "North Korea"
      },
      {
        "id": "MP",
        "label": "Northern Mariana Islands"
      },
      {
        "id": "NO",
        "label": "Norway"
      },
      {
        "id": "OM",
        "label": "Oman"
      },
      {
        "id": "PK",
        "label": "Pakistan"
      },
      {
        "id": "PW",
        "label": "Palau"
      },
      {
        "id": "PA",
        "label": "Panama"
      },
      {
        "id": "PG",
        "label": "Papua New Guinea"
      },
      {
        "id": "PY",
        "label": "Paraguay"
      },
      {
        "id": "PE",
        "label": "Peru"
      },
      {
        "id": "PH",
        "label": "Philippines"
      },
      {
        "id": "PN",
        "label": "Pitcairn Islands"
      },
      {
        "id": "PL",
        "label": "Poland"
      },
      {
        "id": "PT",
        "label": "Portugal"
      },
      {
        "id": "PR",
        "label": "Puerto Rico"
      },
      {
        "id": "QA",
        "label": "Qatar"
      },
      {
        "id": "CG",
        "label": "Republic of the Congo"
      },
      {
        "id": "RE",
        "label": "Reunion"
      },
      {
        "id": "RO",
        "label": "Romania"
      },
      {
        "id": "RU",
        "label": "Russia"
      },
      {
        "id": "RW",
        "label": "Rwanda"
      },
      {
        "id": "BL",
        "label": "Saint Barthelemy"
      },
      {
        "id": "SH",
        "label": "Saint Helena"
      },
      {
        "id": "KN",
        "label": "Saint Kitts and Nevis"
      },
      {
        "id": "LC",
        "label": "Saint Lucia"
      },
      {
        "id": "MF",
        "label": "Saint Martin"
      },
      {
        "id": "PM",
        "label": "Saint Pierre and Miquelon"
      },
      {
        "id": "VC",
        "label": "Saint Vincent and the Grenadines"
      },
      {
        "id": "WS",
        "label": "Samoa"
      },
      {
        "id": "SM",
        "label": "San Marino"
      },
      {
        "id": "ST",
        "label": "Sao Tome and Principe"
      },
      {
        "id": "SA",
        "label": "Saudi Arabia"
      },
      {
        "id": "SN",
        "label": "Senegal"
      },
      {
        "id": "RS",
        "label": "Serbia"
      },
      {
        "id": "SC",
        "label": "Seychelles"
      },
      {
        "id": "SL",
        "label": "Sierra Leone"
      },
      {
        "id": "SG",
        "label": "Singapore"
      },
      {
        "id": "SK",
        "label": "Slovakia"
      },
      {
        "id": "SI",
        "label": "Slovenia"
      },
      {
        "id": "SB",
        "label": "Solomon Islands"
      },
      {
        "id": "SO",
        "label": "Somalia"
      },
      {
        "id": "ZA",
        "label": "South Africa"
      },
      {
        "id": "KR",
        "label": "South Korea"
      },
      {
        "id": "SS",
        "label": "South Sudan"
      },
      {
        "id": "ES",
        "label": "Spain"
      },
      {
        "id": "LK",
        "label": "Sri Lanka"
      },
      {
        "id": "SD",
        "label": "Sudan"
      },
      {
        "id": "SR",
        "label": "Suriname"
      },
      {
        "id": "SZ",
        "label": "Swaziland"
      },
      {
        "id": "SE",
        "label": "Sweeden"
      },
      {
        "id": "CH",
        "label": "Switzerland"
      },
      {
        "id": "SY",
        "label": "Syria"
      },
      {
        "id": "TW",
        "label": "Taiwan"
      },
      {
        "id": "TJ",
        "label": "Tajikistan"
      },
      {
        "id": "TZ",
        "label": "Tanzania"
      },
      {
        "id": "TH",
        "label": "Thailand"
      },
      {
        "id": "TG",
        "label": "Togo"
      },
      {
        "id": "TK",
        "label": "Tokelau"
      },
      {
        "id": "TT",
        "label": "Trinidad and Tobago"
      },
      {
        "id": "TN",
        "label": "Tunisia"
      },
      {
        "id": "TR",
        "label": "Turkey"
      },
      {
        "id": "TM",
        "label": "Turkmenistan"
      },
      {
        "id": "TV",
        "label": "Tuvalu"
      },
      {
        "id": "UG",
        "label": "Uganda"
      },
      {
        "id": "UA",
        "label": "Ukraine"
      },
      {
        "id": "AE",
        "label": "United Arab Emirates"
      },
      {
        "id": "GB",
        "label": "United Kingdom"
      },
      {
        "id": "US",
        "label": "United States"
      },
      {
        "id": "UY",
        "label": "Uruguay"
      },
      {
        "id": "UZ",
        "label": "Uzbekistan"
      },
      {
        "id": "VU",
        "label": "Vanuatu"
      },
      {
        "id": "VA",
        "label": "Vatican"
      },
      {
        "id": "VE",
        "label": "Venezuela"
      },
      {
        "id": "VN",
        "label": "Vietnam"
      },
      {
        "id": "EH",
        "label": "Western Sahara"
      },
      {
        "id": "YE",
        "label": "Yemen"
      },
      {
        "id": "ZM",
        "label": "Zambia"
      },
      {
        "id": "ZW",
        "label": "Zimbabwe"
      }
    ]

  }
}
