from SPARQLWrapper import SPARQLWrapper, JSON
from SPARQLWrapper import XML, GET, POST, JSON, JSONLD, N3, TURTLE, RDF, SELECT, INSERT
from SPARQLWrapper import URLENCODED, POSTDIRECTLY
from SPARQLWrapper.Wrapper import QueryResult, QueryBadFormed, EndPointNotFound, EndPointInternalError
from collections import Counter
import json
import urllib2
import urllib
import pprint

from slugify import slugify
from numpy.core.defchararray import rfind

EXTRAS_ATTRIBUTES = [ "dataset", \
"publisher", \
"license", \
"source",
"creator",
"contributor",
"modified",
"void#exampleResource", \
"void#sparqlEndpoint", \
"void#dataDump"
]


class JSON_Dataset():
    def __init__(self):
        self.name = ''
        self.title = ''
        self.notes = ''
        self.author = ''
        self.extras = []
        self.resources = []
        self.license = 'cc-by'
   
    def tostring(self):
        return   "[%s] name: [%s] title: [%s] notes: [%s] author: [%s] extras: [%s] resource: [%s]" % (self.__class__.__name__, self.name, self.title , self.notes , self.author, self.extras, self.resources)  
        
    def tojson_without_resource(self):
        return { "name" : str(self.name), "title" :  str(self.title), "notes": str(self.notes), "author": self.author, "extras": self.extras , "license_id"  : self.license}
    
    def tojson_all(self):
        return { "name" : str(self.name), "title" :  str(self.title), "notes": str(self.notes), "author": self.author, "extras": self.extras , "license_id"  : self.license, "resources" : self.resources}

    def tojson_resource(self):
        if len(self.resources) > 0:
            result = self.resources[0]
        else:
            result = { }
            
        return result
    
    def __str__(self):
        return str(self.tostring())


class Uploader(): 
    def __init__(self, url, api_key):
        self.url = url
        self.api_key = api_key
        
        
    def send_request(self, data_string, url):
        request = urllib2.Request(url)
        # Creating a dataset requires an authorization header.
        request.add_header('Authorization', self.api_key)
        # Make the HTTP request.
        response = urllib2.urlopen(request, data_string)
        assert response.code == 200
        # Use the json module to load CKAN's response into a dictionary.
        response_dict = json.loads(response.read())
        assert response_dict['success'] is True
        # package_create returns the created package as its result.
        created_package = response_dict['result']
        return created_package
    
    
    def package_update_resource(self, dataset):
        dataset_dict = dataset.tojson_all()
        data_string = urllib.quote(json.dumps(dataset_dict))
        url = self.url + "/api/action/package_update"
        self.send_request(data_string, url)
        
     
    def create_resource(self, dataset):
        resources = dataset.resources[0]
        data_string = urllib.quote(json.dumps(resources))
        url = self.url + "/api/action/resource_create"
        self.send_request(data_string, url)


    def create_dataset(self, dataset):
        dataset_dict = dataset.tojson_without_resource()
        data_string = urllib.quote(json.dumps(dataset_dict))
        url = self.url + "/api/action/package_create"
        self.send_request(data_string, url)
        

    def package_search(self, dataset):
        dataset_dict = {}
        dataset_dict['q'] = 'name:' + dataset.name
        data_string = urllib.quote(json.dumps(dataset_dict))
        url = self.url + "/api/action/package_search"
        result = self.send_request(data_string, url)        
        if result['count'] > 0:
            found = True
        else:
            found = False
            
        return found
        

class SparqlEndpoint():
    def __init__(self):
        pass

     
    def read(self):
        self.wrapper = SPARQLWrapper(endpoint="http://192.168.128.22:8890/sparql")
        self.wrapper.setQuery('SELECT  *  WHERE {?s a void:Dataset}')
        self.wrapper.setReturnFormat(JSON)
        results = self.wrapper.query().convert()
        # get all datasets into an array
        datasets = [a["s"]["value"] for a in results["results"]["bindings"] ] 
        
        json_datasets = []
        procesed_dataset_name = []
        for dataset in datasets:
            json_dataset = JSON_Dataset()
            query = 'SELECT  *  WHERE {   <%s>  ?p ?o  }' % dataset
            self.wrapper.setQuery(query)
            self.wrapper.setReturnFormat(JSON)
            results = self.wrapper.query().convert()
            
            used_key_extras = []
            for result in results["results"]["bindings"]:
                if  result["p"]["value"] == "http://purl.org/dc/terms/title":
                    title_en = result["o"]["xml:lang"]
                    if title_en == 'en':
                        title = result["o"]["value"]
                        json_dataset.title = title
                        json_dataset.name = slugify(title)
                elif  result["p"]["value"] == "http://purl.org/dc/terms/notes":
                    json_dataset.notes = result["o"]["value"]
                elif  result["p"]["value"] == 'http://purl.org/dc/terms/author':
                    json_dataset.author = result["o"]["value"]
                    
                elif result["p"]["value"] == 'http://rdfs.org/ns/void#dataDump':
                    url = str(result["o"]["value"])
                    pos = url.rfind("/")
                    name_of_file = url [pos + 1:]
                    resource_dict = {"package_id" : json_dataset.name, "url": url, "name" : name_of_file }
                    json_dataset.resources.append(resource_dict)
                else:
                    pos = result["p"]["value"].rfind('/')
                    parsed = result["p"]["value"][pos + 1:]
                    if parsed not in EXTRAS_ATTRIBUTES:
                        continue
                  
                    extras_dict = {'key': parsed, 'value': result["o"]["value"]}

                    if  extras_dict not in json_dataset.extras:
                        if parsed in used_key_extras:
                            # najde dvojicu co tam je a pridaj ju k value
                            # zmerzuje tie [{k:k,v:a } {k:k,v:b}] = [{k:k v:a,b}]
                            for d in json_dataset.extras:
                                if d['key'] == parsed:
                                    d['value'] = d['value'] + ", " + result["o"]["value"]
                        else:   
                            used_key_extras.append(parsed)
                            json_dataset.extras.append(extras_dict) 
                
                            
            if not json_dataset.name in procesed_dataset_name:
                procesed_dataset_name.append(json_dataset.name) 
                json_datasets.append(json_dataset)
                 
        return json_datasets



def main():
    sparqlParser = SparqlEndpoint();
    datasets = sparqlParser.read();
    uploader = Uploader(url='http://192.168.59.103', api_key='6afaea8d-48a4-438a-a55d-13c80428984a')
    for dataset in datasets:
        found = uploader.package_search(dataset)
        if found:
            uploader.package_update_resource(dataset)
        else:
            uploader.create_dataset(dataset)
        
    
    
if __name__ == "__main__":
    main()

        
