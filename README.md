<h1 align="center">
    Corona-Warn-App Log Upload
</h1>

<p align="center">
    <a href="https://github.com/corona-warn-app/cwa-log-upload/commits/" title="Last Commit"><img src="https://img.shields.io/github/last-commit/corona-warn-app/cwa-log-upload?style=flat"></a>
    <a href="https://github.com/corona-warn-app/cwa-log-upload/issues" title="Open Issues"><img src="https://img.shields.io/github/issues/corona-warn-app/cwa-log-upload?style=flat"></a>
    <a href="https://github.com/corona-warn-app/cwa-log-upload/blob/master/LICENSE" title="License"><img src="https://img.shields.io/badge/License-Apache%202.0-green.svg?style=flat"></a>
</p>

<p align="center">
  <a href="#development">Development</a> •
  <a href="#documentation">Documentation</a> •
  <a href="#support-and-feedback">Support</a> •
  <a href="#how-to-contribute">Contribute</a> •
  <a href="#contributors">Contributors</a> •
  <a href="#repositories">Repositories</a> •
  <a href="#licensing">Licensing</a>
</p>

The goal of this project is to develop the official Corona-Warn-App for Germany based on the exposure notification API
from [Apple](https://www.apple.com/covid19/contacttracing/)
and [Google](https://www.google.com/covid19/exposurenotifications/). The apps (for both iOS and Android) use Bluetooth
technology to exchange anonymous encrypted data with other mobile phones (on which the app is also installed) in the
vicinity of an app user's phone. The data is stored locally on each user's device, preventing authorities or other
parties from accessing or controlling the data. This repository contains the **log upload** for the Corona-Warn-App.

## Status
![ci-master](https://github.com/corona-warn-app/cwa-log-upload/actions/workflows/ci-master.yml/badge.svg)
[![quality gate](https://sonarcloud.io/api/project_badges/measure?project=corona-warn-app_cwa-log-upload&metric=alert_status)](https://sonarcloud.io/dashboard?id=corona-warn-app_cwa-log-upload)
[![coverage](https://sonarcloud.io/api/project_badges/measure?project=corona-warn-app_cwa-log-upload&metric=coverage)](https://sonarcloud.io/dashboard?id=corona-warn-app_cwa-log-upload)
[![bugs](https://sonarcloud.io/api/project_badges/measure?project=corona-warn-app_cwa-log-upload&metric=bugs)](https://sonarcloud.io/dashboard?id=corona-warn-app_cwa-log-upload)

## About this component

The log upload service is the counterpart of the log upload in the app. 
It enables the App developers to analyse the log data uploaded to the CWA infrastructure to identify the root cause of bugs that only occur in rare conditions are not easy to reproduce. 
The log upload and viewer service follows the privacy preserving paradigm of the corona warn app, only allowing authorized personnel to access the logs.
The means to restrict access and control who can access the data. The logs are kept inside the application and are only accessible trough the portal. 
The access to the portal is restricted trough the IAM component with a separate role for this access. 

## Development

This component can be built locally in order to test the functionality of the interfaces and verify the concepts it is
build upon. There are two ways to build:

- [Maven](https:///maven.apache.org) build - to run this component as spring application on your local machine
- [Docker](https://www.docker.com) build - to run it as docker container build from the provided docker
  build [file](https://github.com/corona-warn-app/cwa-log-upload/blob/master/Dockerfile)

### Prerequisites

[Open JDK 11](https://openjdk.java.net)  
[Maven](https://maven.apache.org)
*(optional)*: [Docker](https://www.docker.com)

### Build

Whether you cloned or downloaded the 'zipped' sources you will either find the sources in the chosen checkout-directory
or get a zip file with the source code, which you can expand to a folder of your choice.

In either case open a terminal pointing to the directory you put the sources in. The local build process is described
afterwards depending on the way you choose.

#### Maven based build

For actively take part on the development this is the way you should choose.   
Please check, whether following prerequisites are fulfilled

- [Open JDK 11](https://openjdk.java.net) or a similar JDK 11 compatible VM
- [Maven](https://maven.apache.org)

is installed on your machine.  
You can then open a terminal pointing to the root directory of the log upload and do the following:

    mvn package
    java -jar target/cwa-log-upload-0.0.1-SNAPSHOT.jar  

The log upload will start up and run locally on your machine available on port 8081. Please keep in mind, that you need
another component [cwa-verification-iam] the get this running in a sensable manner.

#### Docker based build

We recommend that you first check the prerequisites to ensure that

- [Docker](https://www.docker.com)

is installed on your machine.

On the command line do the following:

```bash
docker build -f|--file <path to dockerfile>  -t <imagename>  <path-to-log-upload-root>
docker run -p 127.0.0.1:8085:8085/tcp -it <imagename>
```

or simply

```bash
docker build --pull --rm -f "Dockerfile" -t cwa-log-upload "."
docker run -p 127.0.0.1:8085:8085/tcp -it cwa-log-upload
```

if you are in the root of the checked out repository.  
The docker image will then run on your local machine on port 8085 assuming you configured docker for shared network
mode.

## Code of Conduct

This project has adopted the [Contributor Covenant](https://www.contributor-covenant.org/) in version 2.0 as our code of
conduct. Please see the details in our [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md). All contributors must abide by the code
of conduct.

## Working Language

We are building this application for Germany. We want to be as open and transparent as possible, also to interested
parties in the global developer community who do not speak German. Later on this application might also serve as a
template for other projects outside of Germany. For these reasons, we decided to apply _English_ as the primary project
language.

Consequently, all content will be made available primarily in English. We also ask all interested people to use English
as language to create issues, in their code (comments, documentation etc.) and when you send requests to us. The
application itself, documentation and all end-user facing content will - of course - be made available in German (and
probably other languages as well). We also try to make some developer documentation available in German, but please
understand that focussing on the _Lingua Franca_ of the global developer community makes the development of this
application as efficient as possible.

## Documentation

The full documentation for the Corona-Warn-App can be found in
the [cwa-documentation](https://github.com/corona-warn-app/cwa-documentation) repository. The documentation repository
contains technical documents, architecture information, and white papers related to this implementation.

## Support and Feedback

The following channels are available for discussions, feedback, and support requests:

| Type                     | Channel                                                |
| ------------------------ | ------------------------------------------------------ |
| **General Discussion**   | <a href="https://github.com/corona-warn-app/cwa-documentation/issues/new/choose" title="General Discussion"><img src="https://img.shields.io/github/issues/corona-warn-app/cwa-documentation/question.svg?style=flat-square"></a> </a>   |
| **Concept Feedback**    | <a href="https://github.com/corona-warn-app/cwa-documentation/issues/new/choose" title="Open Concept Feedback"><img src="https://img.shields.io/github/issues/corona-warn-app/cwa-documentation/architecture.svg?style=flat-square"></a>  |
| **Log Upload Issue**    | <a href="https://github.com/corona-warn-app/cwa-log-upload/issues" title="Open Issues"><img src="https://img.shields.io/github/issues/corona-warn-app/cwa-log-upload?style=flat"></a>  |
| **Other Requests**    | <a href="mailto:cwa-opensource@telekom.de" title="Email CWA Team"><img src="https://img.shields.io/badge/email-CWA%20team-green?logo=mail.ru&style=flat-square&logoColor=white"></a>   |

## How to Contribute

Contribution and feedback is encouraged and always welcome. For more information about how to contribute, the project
structure, as well as additional contribution information, see our [Contribution Guidelines](./CONTRIBUTING.md). By
participating in this project, you agree to abide by its [Code of Conduct](./CODE_OF_CONDUCT.md) at all times.

## Contributors

The German government has asked SAP AG and Deutsche Telekom AG to develop the Corona-Warn-App for Germany as open source
software. Deutsche Telekom is providing the network and mobile technology and will operate and run the backend for the
app in a safe, scalable and stable manner. SAP is responsible for the app development, its framework and the underlying
platform. Therefore, development teams of SAP and Deutsche Telekom are contributing to this project. At the same time
our commitment to open source means that we are enabling -in fact encouraging- all interested parties to contribute and
become part of its developer community.

## Repositories

A list of all public repositories from the Corona-Warn-App can be found [here](https://github.com/corona-warn-app/cwa-documentation/blob/master/README.md#repositories).

[cwa-log-upload]: https://github.com/corona-warn-app/cwa-log-upload


## Licensing

Copyright (c) 2020-2023 Deutsche Telekom AG.

Licensed under the **Apache License, Version 2.0** (the "License"); you may not use this file except in compliance with
the License.

You may obtain a copy of the License at https://www.apache.org/licenses/LICENSE-2.0.

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "
AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the [LICENSE](./LICENSE) for
the specific language governing permissions and limitations under the License.
