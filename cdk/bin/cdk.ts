import "source-map-support/register";
import { GuRoot } from "@guardian/cdk/lib/constructs/root";
import { ContentApiFirehoseClientTesting } from "../lib/content-api-firehose-client-testing";

const app = new GuRoot();
new ContentApiFirehoseClientTesting(app, "ContentApiFirehoseClientTesting-euwest-1-INFRA", { stack: "content-api-firehose-client", stage: "INFRA", env: { region: "eu-west-1" } });
