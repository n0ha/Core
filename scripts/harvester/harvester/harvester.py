from SPARQLWrapper import SPARQLWrapper, JSON
from SPARQLWrapper import XML, GET, POST, JSON, JSONLD, N3, TURTLE, RDF, SELECT, INSERT
from SPARQLWrapper import URLENCODED, POSTDIRECTLY
from SPARQLWrapper.Wrapper import QueryResult, QueryBadFormed, EndPointNotFound, EndPointInternalError
from JSON_dataset import JSON_Dataset
from uploader import Uploader
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
#    for dataset in datasets:
#        found = uploader.package_search(dataset)
#        if found:
#            uploader.package_update(dataset)
#        else:
#            uploader.create_package(dataset)
        
        
    for dataset in datasets:
        found, resource_id = uploader.search_resource(dataset)
        
        if found:
            uploader.resource_update(dataset, resource_id)
        else:
            uploader.create_resource(dataset)
        
    
    
if __name__ == "__main__":
    main()

        
