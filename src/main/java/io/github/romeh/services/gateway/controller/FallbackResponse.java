package io.github.romeh.services.gateway.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class FallbackResponse {

	private Integer msgCode;
	private String msg;

}
