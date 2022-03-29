
# agent-overseas-frontend

This frontend allows overseas agents to submit an application to HMRC for registering as an overseas agent and subscribing to Agent Services.

## Journey 
1. The agent submits an application - The journey requires several types of documents to be uploaded as evidence. The file upload function is performed by the UPSCAN service. 

2. The application is sent the HMRC Agent Maintainer Team where helpdesk staff with the correct stride role manaully check the application, and approve or reject it via agent-helpdesk-frontend. The status is updated from "pending" see [agent-overseas-application](https://github.com/hmrc/agent-overseas-application#application-statuses) for different statuses.

3. The agent comes back, and they can see the status of their application. Once they have been accepted they're asked to confirm their agency details. This allows them to finish 'creating their account' and the confirmation screen lets them go to their new Agent Services Account home page.



## Running the tests

    sbt test it:test

## Running the tests with coverage

    sbt clean coverageOn test it:test coverageReport

## Running the app locally

    sm --start AGENT_ONBOARDING -r
    sm --stop AGENT_OVERSEAS_FRONTEND
    sbt run

It should then be listening on port 9414

    browse http://localhost:9414/agent-services/apply-from-outside-uk
    browse http://localhost:9414/agent-services/apply-from-outside-uk/create-account
    
### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
