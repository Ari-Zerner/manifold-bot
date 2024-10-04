# Configure the Google Cloud provider
provider "google" {
  project = var.project_id
  region  = var.region
}

# Create a Google Compute Engine instance
resource "google_compute_instance" "manifold_bot_instance" {
  name         = "manifold-bot-instance"
  machine_type = "e2-micro"
  zone         = "${var.region}-a"

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-11"
    }
  }

  network_interface {
    network = "default"
    access_config {
      // Ephemeral IP
    }
  }

  metadata_startup_script = <<-EOF
    #!/bin/bash
    apt-get update
    apt-get install -y openjdk-11-jdk git curl

    # Install Google Cloud Logging agent
    curl -sSO https://dl.google.com/cloudagents/add-logging-agent-repo.sh
    bash add-logging-agent-repo.sh
    apt-get update
    apt-get install -y google-fluentd

    # Install Leiningen
    curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein -o /usr/local/bin/lein
    chmod a+x /usr/local/bin/lein

    # Install my dotfiles for when I SSH in
    eval "$(curl https://raw.githubusercontent.com/Ari-Zerner/.dotfiles/master/init)"

    git clone https://github.com/ari-zerner/manifold-bot.git
    cd manifold-bot
    gsutil cp gs://manifold-bot-config/config.edn .
    lein run
  EOF

  service_account {
    scopes = ["cloud-platform"]
  }
}

# Define variables
variable "project_id" {
  description = "Google Cloud Project ID"
}

variable "region" {
  description = "Google Cloud region"
  default     = "us-central1"
}

# Output the instance IP
output "instance_ip" {
  value = google_compute_instance.manifold_bot_instance.network_interface[0].access_config[0].nat_ip
}
