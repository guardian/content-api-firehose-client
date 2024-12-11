import { App } from "aws-cdk-lib";
import { Template } from "aws-cdk-lib/assertions";
import { ContentApiFirehoseClientTesting } from "./content-api-firehose-client-testing";

describe("The ContentApiFirehoseClientTesting stack", () => {
  it("matches the snapshot", () => {
    const app = new App();
    const stack = new ContentApiFirehoseClientTesting(app, "ContentApiFirehoseClientTesting", { stack: "content-api-firehose-client", stage: "TEST" });
    const template = Template.fromStack(stack);
    expect(template.toJSON()).toMatchSnapshot();
  });
});
