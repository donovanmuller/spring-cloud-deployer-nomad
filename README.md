# Spring Cloud Data Flow Nomad

An implementation of the [spring-cloud-deployer](https://github.com/spring-cloud/spring-cloud-deployer) 
SPI for scheduling applications with [Hashicorp Nomad](https://www.nomadproject.io/).

This implementation borrows heavily from the [spring-cloud-deployer-kubernetes](https://github.com/spring-cloud/spring-cloud-deployer-kubernetes)
project.

## nomad-api

The Nomad [HTTP API](https://www.nomadproject.io/docs/http/index.html)
is consumed using the [nomad-api](https://github.com/zanella/nomad-api)
project by [Rafael Zanella](https://github.com/zanella). Huge thanks for the initial implementation.

Some missing API models have been added and others altered for use with the latest
Nomad HTTP API. The changes have been committed on [this fork](https://github.com/donovanmuller/nomad-api). 
A pull request ([#1](https://github.com/zanella/nomad-api/pull/1)) has been opened with these changes.

## Building

### nomad-api fork
 
Due to the modifications needed on the `nomad-api` project and in lieu of the open pull request being merged
in the upstream project, the fork for `nomad-api` must be built and installed before building the Nomad deployer:

```bash
$ git clone https://github.com/donovanmuller/nomad-api.git
$ ./mvnw install
```

### Nomad deployer

If you don't have a Nomad instance available to run the integration tests as part of the build, 
you can skip the tests and build the Nomad deployer with:

```bash
$ ./mvnw install -DskipTests 
```

## Integration tests

### Hashistack Vagrant box

If you require a local Nomad instance to run the integration tests, you can use the [hashistack-vagrant](https://github.com/donovanmuller/hashistack-vagrant)
project to stand up Nomad and accompanying tools. Assuming you have [Vagrant](https://www.vagrantup.com)
installed, clone the `hashistack-vagrant` project and follow the below steps:

```bash
$ git clone https://github.com/donovanmuller/hashistack-vagrant.git
$ cd hashistack-vagrant
$ vagrant plugin install landrush # requires the 'landrush' plugin
$ vagrant up
$ vagrant ssh
...
vagrant@hashistack:~$ tmuxp load full-hashistack.yml
...
```

by default this will stand up a Nomad instance on `172.16.0.2:4646`/`nomad-client.hashistack.vagrant:4646`. 

Please see [the hashistack-vagrant](https://github.com/donovanmuller/hashistack-vagrant.git) 
GitHub project for more details.

### Running the tests

Assuming you have access to a Nomad instance (see above if you don't), you can run the integration tests with:

```bash
$ ./mvnw test \
  -Dspring.cloud.deployer.nomad.nomadHost=172.16.0.2 \
  -Dspring.cloud.deployer.nomad.nomadPort=4646
```

where `-Dspring.cloud.deployer.nomad.nomadHost` and `-Dspring.cloud.deployer.nomad.port` 
optionally specify the host and port where a Nomad client is listening.
The default values are `localhost` and `4646` respectively.







