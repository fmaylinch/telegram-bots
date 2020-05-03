import subprocess
import json

# TODO: Note this script kills the task. Because I can't restart it other way.
#       `aws ecs update-service --force-new-deployment` doesn't work without killing the task first.
#       See my comments in this post answer: https://stackoverflow.com/a/58204611/1121497

task_definition = "LanXatBot-TaskDefinition"
service = "may-lanxbot-service"
cluster = "may-ec2-cluster-2"

# list-tasks

command = "aws ecs list-tasks --cluster " + cluster + " --service-name " + service
ls_out = subprocess.Popen(command.split(), stdout=subprocess.PIPE)
stdout, stderr = ls_out.communicate()
# print("Result of list-tasks: " + stdout)

list_task_result = json.loads(stdout)
tasks = list_task_result["taskArns"]

if len(tasks) > 1:
    print("More than one task running. I don't know which one to stop.")
    exit()


if len(tasks) == 0:
    print("No tasks running")

else:
    # stop-task

    task_id = tasks[0]
    print("Stopping task: " + task_id)

    command = "aws ecs stop-task --cluster " + cluster + " --task " + task_id
    ls_out = subprocess.Popen(command.split(), stdout=subprocess.PIPE)
    stdout, stderr = ls_out.communicate()
    # print("Result of stop-task: " + stdout)


# update-service

print("In a few seconds, the service should restart the task definition: " + task_definition)

# --- This is not necessary, service should restart the task ---
# command = "aws ecs update-service --force-new-deployment --service " + service + " --cluster " + cluster + " --task-definition " + task_definition
# ls_out = subprocess.Popen(command.split(), stdout = subprocess.PIPE)
# stdout, stderr = ls_out.communicate()
# print("Result of update-service: " + stdout)
