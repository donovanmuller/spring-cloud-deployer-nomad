# Spring Cloud Data Flow Nomad

An implementation of the [spring-cloud-deployer](https://github.com/spring-cloud/spring-cloud-deployer) 
SPI for scheduling applications with [Hashicorp Nomad](https://www.nomadproject.io/).

Please see the [Spring Cloud Data Flow Server Nomad](https://github.com/donovanmuller/spring-cloud-dataflow-server-nomad)
for a runtime implementation of this deployer SPI implementation.

This implementation borrows heavily from the [spring-cloud-deployer-kubernetes](https://github.com/spring-cloud/spring-cloud-deployer-kubernetes)
project.

## nomad-api

The Nomad [HTTP API](https://www.nomadproject.io/docs/http/index.html)
is consumed using the [nomad-api](https://github.com/zanella/nomad-api)
project by [Rafael Zanella](https://github.com/zanella). 
Huge thanks for the already having done all the hard work.

## Building

If you don't have a Nomad/Consul instance available to run the integration tests as part of the build, 
you can skip the integration tests and build the Nomad deployer with:

```bash
$ ./mvnw install -Dnomad.enabled=false
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
  -Dspring.cloud.deployer.nomad.nomadPort=4646 \
  -Dspring.cloud.deployer.nomad.deployerHost=<local machines IP>
```

where `-Dspring.cloud.deployer.nomad.nomadHost` and `-Dspring.cloud.deployer.nomad.port` 
optionally specify the host and port where a Nomad client is listening.  The default values are `localhost` and `4646` respectively.

The `spring.cloud.deployer.nomad.deployerHost`
value must be set for the Maven resource support. The value should be the accessible (from the VM) IP address of your local machine
from where you run the tests.







