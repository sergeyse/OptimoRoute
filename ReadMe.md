# Integration of track optimization SAAS Optiroute with ERP. 

## Resources 

- API  WS key optimoroute:****
- Create record https://api.optimoroute.com/v1/create_order?key=*****
   
   -Body for a new record :
   
   {   "operation": "MERGE",   "orderNo": "555555",   "type": "D",   "date": "2021-03-04",   "location": {     "address": "KÓPALIND 3",     "locationNo": "1111464119",     "locationName": "HÁBERG 7",     "acceptPartialMatch": true   },   "duration": 20,   "twFrom": "10:00",   "twTo": "10:59",   "load1": 1,   "load2": 2,   "notes": "Deliver at back door" }
   
  
