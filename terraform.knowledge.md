# Terraform and Google Cloud Deployment

## Setup
1. Install Terraform on your local machine
2. Set up a Google Cloud project and enable necessary APIs
3. Create `main.tf` and `terraform.tfvars` in project root

## Initialization and Deployment
1. Initialize Terraform:
   ```
   terraform init
   ```
2. Review planned changes:
   ```
   terraform plan
   ```
3. Apply configuration:
   ```
   terraform apply
   ```

## Important Notes
- Ensure `project_id` in `terraform.tfvars` matches your Google Cloud project
- Update GitHub repository URL in `main.tf` startup script
- May need further customization (e.g., service account, firewall rules)

## VM Configuration
- Install necessary dependencies for running the Clojure application:
  - Java JDK
  - Git
  - Leiningen (Clojure build tool)
- Leiningen is not available by default on Debian-based systems
- Install Leiningen in the startup script:
  ```bash
  # Install Leiningen
  curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/local/bin/lein
  chmod a+x /usr/local/bin/lein
  ```
- Update PATH to include Leiningen:
  ```bash
  export PATH=$PATH:/usr/local/bin
  ```
## Logging
- Configure application to use Google Cloud Logging
- Logs will be accessible via Google Cloud Console
- Use appropriate logging library compatible with Google Cloud Logging (e.g., Logback with Google Cloud Logging appender)

## Version Control
- Add Terraform-specific entries to .gitignore:
  ```
  # Terraform files
  .terraform/
  *.tfstate
  *.tfstate.*
  crash.log
  *.tfvars
  override.tf
  override.tf.json
  *_override.tf
  *_override.tf.json
  .terraform.lock.hcl
  ```
- Commit `main.tf` and other Terraform configuration files
- Do not commit `terraform.tfvars` if it contains sensitive information
- The `.terraform.lock.hcl` file should be committed to version control as it ensures consistent provider versions across team members and environments

## Reference
- [Terraform Documentation](https://www.terraform.io/docs)
- [Google Cloud Terraform Provider](https://registry.terraform.io/providers/hashicorp/google/latest/docs)
- [Google Cloud Logging](https://cloud.google.com/logging/docs)
- [Terraform .gitignore Template](https://github.com/github/gitignore/blob/main/Terraform.gitignore)
- Commit `.terraform.lock.hcl` to ensure consistent provider versions across environments
