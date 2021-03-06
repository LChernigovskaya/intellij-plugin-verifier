package org.jetbrains.plugins.verifier.service.server.controllers

import org.jetbrains.plugins.verifier.service.server.ServerContext
import org.jetbrains.plugins.verifier.service.server.views.StatusPage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class StatusPageController {
  @Autowired
  private lateinit var serverContext: ServerContext

  @GetMapping("/")
  fun statusPageEndpoint() = StatusPage(serverContext)
}