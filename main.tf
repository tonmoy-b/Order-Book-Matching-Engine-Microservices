terraform {
  required_providers {
    keycloak = {
      source  = "keycloak/keycloak"
      version = ">= 4.4.0"
    }
  }
}

variable "kc_admin_user" { type = string }
variable "kc_admin_password" { type = string }
variable "client_secret" {
  type      = string
  sensitive = true
}

provider "keycloak" {
  client_id     = "admin-cli"
  username      = var.kc_admin_user     # Keycloak admin username
  password      = var.kc_admin_password     # Keycloak admin password
  url           = "http://keycloak:8080"
}

# create a realm
resource "keycloak_realm" "orderbook_realm" {
  realm   = "orderbook-realm"
  enabled = true
  display_name = "Orderbook Production"
}

# create a client
resource "keycloak_openid_client" "orderbook_client" {
  realm_id  = keycloak_realm.orderbook_realm.id
  client_id = "orderbook-service"
  enabled   = true
  access_type = "CONFIDENTIAL"
  client_secret = var.client_secret
  # enable one of the flows
  standard_flow_enabled = true           #  standard flow (authorization code flow)
  direct_access_grants_enabled = true  # Resource Owner Password Credentials flow
  # implicit_flow_enabled = false        # implicit flow -- note:this is not recommended
  # service_accounts_enabled = true      # client credentials flow


  valid_redirect_uris = [
    "http://localhost:8080/*"
  ]
}

# define roles
resource "keycloak_role" "roles" {
  for_each = toset(["ADMIN", "TRADER", "VIEWER"])

  realm_id = keycloak_realm.orderbook_realm.id
  name     = each.key
}

# define users
variable "users" {
  default = {
    "testuser" = {
      first_name = "Test",
      last_name  = "User",
      email = "testuser@example.com",
      pass  = "password123",
      email_verified = true,
      roles = ["TRADER", "VIEWER"]
    },
    "admin" = {
      first_name = "admin",
      last_name = "admin",
      email = "admin@example.com",
      pass  = "admin123",
      email_verified = true,
      roles = ["ADMIN", "TRADER", "VIEWER"]
    },
    "viewer" = {
      first_name = "viewer",
      last_name = "viewer",
      email = "viewer@example.com",
      pass  = "viewer123",
      email_verified = true,
      roles = ["VIEWER"]
    }
  }
}

# create users
resource "keycloak_user" "orderbook_users" {
  for_each = var.users

  realm_id = keycloak_realm.orderbook_realm.id
  username = each.key
  first_name = each.value.first_name
  last_name  = each.value.last_name
  enabled  = true
  email    = each.value.email
  email_verified = true

  initial_password {
    value     = each.value.pass
    temporary = false
  }
}

# map roles to users
resource "keycloak_user_roles" "user_roles" {
  for_each = var.users

  realm_id = keycloak_realm.orderbook_realm.id
  user_id  = keycloak_user.orderbook_users[each.key].id

  role_ids = [
    for r in each.value.roles : keycloak_role.roles[r].id
  ]
}