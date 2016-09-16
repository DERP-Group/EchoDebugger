Allows the RESTful submission of Echo service responses, which can then be "played" by the Echo calling the service.

This lets us rapidly test SSML pronunciations, timing, and delivery of content.

## Posting a response to your Echo
```
POST /echodebugger/user/{echoId}/
{
  "version": null,
  "response": {
    "outputSpeech": {
      "type": "SSML",
      "id": null,
      "ssml": "<speak><phoneme alphabet=\"ipa\" ph=\"kɒmpləbɑt\"> complibot </phoneme> is awesome!</speak>"
    },
    "card": {
      "type": "Simple",
      "title": "This is a sample response",
      "content": "This is a sample response"
    },
    "reprompt": null
  },
  "sessionAttributes": null
}
```

## Posting a response to your Echo with just the plainText response to speak
```
POST /echodebugger/user/{echoId}/plainText
This is some plain text for the echo to speak
```

## Posting a response to your Echo with a Ssml response to speak
```
POST /echodebugger/user/{echoId}/plainText
<speak><p>This is some plain text,<break /> the echo should interpret it properly</p></speak>
```

## Getting your current response
`
GET /echodebugger/user/{echoId}/
`

## Getting all responses
`
GET /echodebugger/user/?showAllUsers=true
`