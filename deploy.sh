echo --- Generating JAR ---
mvn clean package

echo --- Building Docker image ---
docker build -t fmaylinch/lanxatbot .

echo --- Pushing Docker image ---
docker push fmaylinch/lanxatbot

echo --- Restarting ECS Service ---
python restart_aws_service.py
