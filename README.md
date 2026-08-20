# File Storage Service

A REST API for uploading, storing, and managing files, built with Spring Boot and deployed on AWS. Files are stored in Amazon S3; metadata (filename, size, content type, upload time) is tracked in MySQL.

**Live demo:** `http://13.201.131.201:8080/api/files` *(EC2 instance — may be stopped when not actively demoing; contact me to spin it back up)*

## Tech stack

- **Backend:** Java 21, Spring Boot 4, Spring Data JPA
- **Database:** MySQL
- **Storage:** AWS S3 (via AWS SDK for Java v2)
- **Deployment:** AWS EC2, provisioned with CloudFormation (infrastructure-as-code)

## Architecture : 
Client (Postman / frontend)
│
▼
Spring Boot REST API (EC2)
│
├──► AWS S3 (file storage)
└──► MySQL (on EC2) (metadata: filename, size, content type, upload timestamp)


## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/files/upload` | Upload a file (multipart form-data, key: `file`) |
| GET | `/api/files` | List all uploaded files |
| GET | `/api/files/{id}/download` | Download a specific file |
| DELETE | `/api/files/{id}` | Delete a file |

## Running locally

**Prerequisites:** Java 21, MySQL, an AWS account with an S3 bucket and an IAM user with S3 permissions.

1. Create the database:
```sql
   CREATE DATABASE filestorageservice;
```

2. Set environment variables (never hardcoded in source):
   AWS_ACCESS_KEY_ID=your-key
   AWS_SECRET_ACCESS_KEY=your-secret
   DB_PASSWORD=your-mysql-password

   
3. Update `application.properties` with your S3 bucket name and AWS region.

4. Run: ./mvnw spring-boot:run

## Deployment

Infrastructure is provisioned via CloudFormation (`deploy/cloudformation-template.yaml`), which creates:
- An EC2 instance (t3.micro) running Amazon Linux 2023
- A security group allowing SSH (22) and app traffic (8080)
- MySQL (MariaDB) installed and initialized automatically on instance launch

The Spring Boot app is packaged as a JAR, transferred to the instance via `scp`, and run in the background with `nohup` so it stays up after the SSH session ends.

## Security notes

- AWS credentials and database password are passed via environment variables — never committed to source control
- S3 bucket has "Block all public access" enabled; files are only reachable through the API
- Each uploaded file gets a UUID-prefixed S3 key to prevent collisions and avoid overwrites
- A dedicated MySQL user with limited privileges (not root) is used in the deployed environment

## Notes

This project was built end-to-end as a learning exercise in cloud deployment — including working through real issues along the way: CloudFormation instance-type/free-tier constraints,
SSH key path and terminal encoding issues, an AWS credential leak caught and resolved via GitHub's push protection (keys rotated immediately),
and Hibernate dialect/JDBC connection troubleshooting between local and deployed environments.

One thing worth deciding before you finalize this: do you want to keep the live EC2 IP in the README (meaning you'd need to keep the instance running, or update the IP if you restart it later), 
or would you rather remove the "Live demo" line entirely and just describe the deployment without a clickable link? Either is fine — just let me know and I'll adjust.
