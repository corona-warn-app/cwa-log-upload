# Software Design Log upload service, Logviewer


##	Introduction
This document describes the component Log upload service + Logviewer for the System “Corona Warn App”. In the world of the Corona Warn App the Log upload service + Log viewer helps to validate whether upload requests from the mobile App are valid or not.
This document links the overall system architecture with the software design of the Log upload service + Log viewer, it links user stories with implementation inside the Log upload service + Log viewer.
This document is intended to be read by people who want to get insights how verification works in detail, it is our guideline for implementation.

#	Overview
##	Purpose of the Software System Component
The primary scope of the component is to enable users to upload logfiles collected on their devices to the cwa and give designated app developers access to them.

##	Context
The Log upload service + Log viewer works in conjunction with the Coronawarn.app Android and iPhone app. On user request, the phone collects runtime data, that are specific to the current phone that help to evaluate errors in rare conditions.

##	Core Entities
| Entity |	Definition |	
| ------------- |:-------------:|
| Log collection |	component on the smart phone, that controlls and monitors the collection of log data and enables the user to transmit them	|
| Log upload service |	CWA component, that provides an interface for the logs to be uploaded and stores them on a object data storage in the open telekom cloud	|
| TOTP |	This is a onetime token that the clients sends in the http header. The log upload service can verify this token against the PPA Server for validity	|
| Log viewer |	The log viewer provides a log on interface to authenticate a user (app developer) and presents the uploaded logs, provides full text and regex search |
|CWA IAM|	This is the identity and access management component, that is also used for the verification portal. It contains an additional group that will hold the developers who will have access to the log viewer	|

# Software Design

##	Privacy Constraints
The Log upload service receives zipped log data, which may contain certain information about the participation user
 - ip address
 - number of risk encounters
 t.b.c

### Measures
|ID|	Measure	|Comment|
| ------------- |:-------------:| -----:|
|M1|	all data is enclosed in a zipped file and is stored in a obs bucket on the OTC, only a technical user can access this bucket	|
|M2|	periodical deletion of data 	|  all data is deleted after an configurable amaout of time, the databse entries, as well as the corresponding OBS objects |
|M3|	downloaded data is processed in the context of the webbrows, i. e. that the downloaded zipped logfiles will be deleted when the browser session is closed	|


##	Measures to increase data privacy
###	Separate Operation of log upload, log viewer Service and Authentification service
The log upload and log viewer will be handled as two seperate services. The may by that means have seperate routing and network configurtaion like ip whitelisting for the log viewer.

###	Logging
Logging will be reduced to indication of an successful data upload for the log upload service, and information of deletion  The log viewer will have log

##	Important Assumptions


##	Actors
- **User/Coronawarn.app Nutzer**: Person, who is tested for SARS-CoV-2, is equipped with a smartphone, Corona Warn App installed 
- **User/Backend Developer**: Person who reads log files to analyse seldom failure situations occuring on the corona.app user's phone

##	Big Picture - User Journeys
###	User Journey Log upload 

Note: 

Steps:


###	User Journey Log viewing
 
 Precondition:

Steps

##	Supported User Stories 


##	Implemented Use Cases
###	Use Case Log upload
API Endpoint:
-	Method: POST /logs
Body: { 
"key": "<< key >>",
"keyType": “teleTAN||hashedGUID” 
}
-	Authentication: TOTP via http header
Steps:

###	Use Case Log view

Steps

###	Use Case Delete data to keep data privacy high
Steps:
1.	Delete all entities OBS objects older than 2 weeks
2.	Delete all entities in database oder than 2 weeks



#	Data Model
The data model is persisted in a dedicated schema.
##	Entity Logs
The entity logs represents the link to the zipped log files in OTC storage

|Name|	Not null|	Type|	Definition|
| ------------- |:-------------:| -----:| -----:|
|id|	Y|	String[32]|	Primary key, row index|
|created_at|	Y|	Date|	Timestamp when the entry is stored|
|filename|	Y|	String[100]|	Filename |
|size|	Y|	long|	Lenght of zipfile|
|hash	|Y	|String[32] |	Hash |
|metadata|	N|	String[1000] |	Metadata|

###	Data Deletion
All data is deleted after 21 days.

##	Entity AppSession
The entity AppSession is a hashed GUID which was used in processing to generate a TAN. The entity basically marks a GUID hash as “used”.

|Name|	Not null|	Type|	Definition|
| ------------- |:-------------:| -----:| -----:|
|GUIDHash||	String[64]|	The hashed GUID.|
|teleTANHash||	String[64]|	The hashed teleTAN.|
|RegistrationTokenHash|	Y|	String[64]|	Hash of the Registration Token.|
|TANcounter|	Y|Int|	Contains the number of TANs generated in the session|
|sourceOfTrust|	Y|	String [“hashedGUID”, “teleTAN”]|Defines the type of the Session|
|createdON|	Y|	Date	|Date of creation|

###	Data Deletion
All data is deleted after 14 days.

# Security
##	Authentication
###	Authentication of users
|Role|	Authentication	|Comment|
| ------------- |:-------------:| -----:|
|Anonymous |	None|	 the app uses no authentication for communication with Verification Server|
|Corona Warn App Server|	TLS Client Certificate, 2nd factor IP Range	|
|Health Authority User|	Signed JWT, verification of signature	|
|Hotline User|	Signed JWT, verification of signature	|


###	Authorization of users
|Role|	Authorization|	Comment|
| ------------- |:-------------:| -----:|
|Anonymous| 	None|	the app uses no authorization for communication with Verification Server |
|Corona Warn App Backend User|	Implicit authorization | a user which is authenticated as Corona Warn App Backend User is authorized as Corona Warn App Backend User	|
|Health Authority User|	Signed JWT, verification of signature | Signature contains role “c19healthauthority”	|
|Hotline User|	Signed JWT | verification of signature “c19hotline”	|

## Threat Model
**_This chapter is still in work._**
###	Threats
Based on STRIDE threat modelling, the threats below are anticipated:
|ID|	Category|	Name|	Definition|
| ------------- |:-------------:| -----:| -----:|
|T1|	Brute Force	| Brute Force teleTAN| 	Try to guess a teleTAN via brute force attack.|
|T2|		DDoS Attack|	The API is attacked by a high number of requests, leading to an outage of the service|
|T3|		Code injection|	The payload and/or header contain code which is executed|
|T4|			
|T5|		Brute force attack|	By a brute force attack a client wants to guess a valid GUID to create a valid TAN|
|T6|		Steal secrets from logs	|
			
Categories follow STRIDE:
-	Spoofing
-	Tampering
-	Repudiation
-	Information disclosure (privacy breach or data leak)
-	Denial of service
-	Elevation of privilege

###	Measures

|ID|	Threat|	Name|	Definition|
| ------------- |:-------------:| -----:| -----:|
|MT1|||OTC DDoD Protection	Infrastructure Level|
|MT2|||Strong input parameter verification, with 100% code coverage and very high amount of testing	|
|MT3|||Enforcing TLS 1.2 and above	|
|MT6|T2||	Use Open Telekom Cloud Anti-DDoS	|
|MT7|T3||	Strict validation of http headers, body content	|
|MT9|T5||	Use Throttling @ Code Level in API implementation to reduce the possible frequency of guessing attempts	|
|MT10|T5||	Detect unusual load scenario and trigger warning for operation	|
|MT11|T6||	Use only POST requests to avoid logging of secrets at infrastructure components|
|MT12|||	Strict input validation, all REST input parameter are validated in a strict manner|
			
			

##	Used cryptographic algorithms
- Hashing of GUID: SHA-256, no salt, no pepper
- Hashing of Registration Token: SHA-256, no salt, no pepper
- Hashing of TAN: SHA-256, no salt, no pepper
- Creating of Registration Token: the JAVA UUID generation (UUID.randomUUID()) is used, which relies on Java SecureRandom (see https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/SecureRandom.html ) is used. this random is considered as cryptographically strong random number generator
- Creating of TAN: the JAVA UUID generation (UUID.randomUUID()) is used, which relies on Java SecureRandom (see https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/SecureRandom.html ) is used. this random is considered as cryptographically strong random number generator
- Creating of teleTAN: string of random chars, as random java SecureRandom (see https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/security/SecureRandom.html ) is used. this random is considered as cryptographically strong random number generator

##	Complexity of secrets
- TAN: 128 bits
- Registration Token: 128 bits
- teleTAN: 44 bits (31 characters, length of 9)

## Used Timeframes
TAN
-	Lifespan of TAN is configured to 14 days

teleTAN
-	Lifespan of teleTAN is configured to 1h
