
# agent-overseas-frontend

# Frontend for Overseas Agents to apply to become an Agent and subscribe to Agent Services

This frontend allows overseas agents to submit an application to HMRC for registering as an overseas agent and subscribing to Agent Services.

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