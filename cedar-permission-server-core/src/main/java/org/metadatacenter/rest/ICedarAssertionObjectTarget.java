package org.metadatacenter.rest;

public interface ICedarAssertionObjectTarget {

  void beNull(ICedarOperationDescriptor operationDescriptor);

  void beNotNull(ICedarOperationDescriptor operationDescriptor);
}
