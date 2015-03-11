<div style="margin:10px;">
<h1>Elasticsearch in dotCMS</h1>


<br>&nbsp;<br>
<h2>Examples Queries</h2>
<hr>
These queries can be tested in the Elasticsearch query portlet


<h3>Match every piece of content, show the first 10 results</h3>
<pre><code>{
    "query" : {
        "match_all" : {}
    },
    "size":  10
}
</code></pre>

<br>

<h3>Match every piece of content, paginate showing the results 11-20</h3>
<pre><code>{
    "query" : {
        "match_all" : {}
    },
    "size":  10,
    "from": 10
}
</code></pre>

<br>



<h3>Match the term "gas" across all fields</h3>
<pre><code>{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "_all": "gas"
                }
            }
        }
    }
}
</code></pre>

<br>



<h3>Facet on the news.tags field</h3>
<pre><code>
{
    "query" : {
        "match_all" : {  }
    },
    "facets" : {
        "tag" : {
            "terms" : {
                "field" : "news.tags",
                "size" : 100   //the number of facets to return
            }
        }
    },
	"size":0    //the number of hits to return
}
</code></pre>
<br>


<h3>Suggest based on title, return only the suggestions (size:0))</h3>
<pre><code>{
  "suggest" : {
    "title-suggestions" : {
      "text" : "gs pric rollrcoater",
      "term" : {
        "size" : 10,
        "field" : "title"
      }
    }
  }    
  ,"size":0
}
</code></pre>

<br>

<h3>Query using a range</h3>
<pre><code>{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "title": "gas"
                }
            },
            "must_not": {
                "range": {
                    "languageid": {
                        "from": 2,
                        "to": 20
                    }
                }
            }
        }
    }
}
</code></pre>

<br>&nbsp;<br>

<h2>Viewtool<a name="#esPortletViewtool"></a></h2>
<hr>
<p>This is an example of how you can use the $estool to in Velocity to pull content from dotCMS on a velocity page.</p>
<h3>Pull content where title contains "gas"</h3>
<pre><code>#set($results = $estool.search('{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "title": "gas"
                }
            },
            "must_not": {
                "range": {
                    "languageid": {
                        "from": 2,
                        "to": 20
                    }
                }
            }
        }
    }
}'
))

#foreach($con in $results)
  $con.title&lt;br&gt;
#end
&lt;hr&gt;
$results.response&lt;br&gt;

</code></pre>

<br>&nbsp;<br>

<h2><a name="#esPortletRest"></a>RESTful api</h2>
<hr>
<p>You can also pull contents from elasticsearch using the RESTful api - here we are curling for results from the command line </p>
<pre><code>curl -H "Content-Type: application/json" -XPOST http://localhost:8080/api/es/search -d '{
    "query": {
        "bool": {
            "must": {
                "term": {
                    "_all": "gas"
                }
            }
        }
    }
}'
</code></pre>
<p>curl for facets (the /api/es/raw endpoint gives you the the raw SearchResponse directly from ElasticSearch)</p>
<pre><code>curl -H "Content-Type: application/json" -XPOST http://localhost:8080/api/es/raw -d '
    {
        "query" : { "query_string" : {"query" : "gas*"} },
        "facets" : {
            "tags" : { "terms" : {"field" : "news.tags"} }
        }
    }
'
</code></pre>

<br>


<p>curl for suggestions (Did you mean?)</p>
<pre><code>curl -H "Content-Type: application/json" -XPOST http://localhost:8080/api/es/raw -d '
    {
      "suggest" : {
        "title-suggestions" : {
          "text" : "gs pric rollrcoater",
          "term" : {
            "size" : 3,
            "field" : "title"
          }
        }
      }
    }
'
</code></pre>

<br>&nbsp;<br>

<h2><a name="#esPortletGeo"></a>Geolocation</h2>
<p>For these examples to work you need to add a field to the ""news structure that uses latlon as its velocity variable name. In this example, it is just a text field with a value of ""42.648899,-71.165497)</p>
<p>Filter news by distance away</p>
<pre><code>{
    "query": {
        "filtered": {
            "query": {
                "match_all": {}
            },
            "filter": {
                "geo_distance": {
                    "distance": "20km",
                    "news.latlong": {
                        "lat": 37.776,
                        "lon": -122.41
                    }
                }
            }
        }
    }
}
</code></pre>

<br>

<p>filter news by distance away part 2 (For this example to work you need to add a field to the news structure that uses latlon as its velocity variable name. it can be a text field with a value of ""42.648899,-71.165497)</p>
<pre><code>{
    "query": {
        "filtered": {
            "query": {
                "match_all": {}
            },
            "filter": {
                "geo_distance": {
                    "distance": "20km",
                    "news.latlong": {
                        "lat": 42.648899,
                        "lon": -71.165497
                    }
                }
            }
        }
    }
}
</code></pre>

<br>

<p>sort news by distance away (For this example to work you need to add a field to the news structure that uses latlon as its velocity variable name. it can be a text field with a value of ""42.648899,-71.165497)</p>
<pre><code>{
    "sort" : [
        {
            "_geo_distance" : {
                "news.latlong" : {
                    "lat" : 42,
                    "lon" : -71
                },
                "order" : "asc",
                "unit" : "km"
            }
        }
    ],
    "query" : {
        "term" : { "title" : "gas" }
    }
}</code></pre>


<div style="height:300px"></div>
</div>