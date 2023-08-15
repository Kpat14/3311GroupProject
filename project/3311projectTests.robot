*** Settings ***
Library           Collections
Library           RequestsLibrary
Test Timeout      30 seconds

Suite Setup    Create Session    localhost    http://localhost:8080

*** Test Cases ***

addKevin
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Kevin Bacon    actorId=nm0000102
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200

addFootloose
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Footloose    movieId=foot
    ${resp}=    PUT On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}    expected_status=200

addfakemovie1
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=fake1    movieId=fake1
    ${resp}=    PUT On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}    expected_status=200

addfakemovie2
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=fake2    movieId=fake2
    ${resp}=    PUT On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}    expected_status=200

computeKevinBaconHimselfPath
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=nm0000102
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconPath    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"baconPath":["nm0000102"]}

computeKevinBaconHimselfNumber
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=nm0000102
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconNumber    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"baconNumber":0}

addRelationshipKevinfoot
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=nm0000102   movieId=foot
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addSexandCity
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=SexandCity    movieId=city
    ${resp}=    PUT On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}    expected_status=200

addSarahJessica
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Sarah Jessica   actorId=sarah
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200

addRelationshipJessfoot
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=sarah   movieId=foot
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addRelationshipJesscity
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=sarah   movieId=city
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addKimKatrao
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Kim Katrao    actorId=kim
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200

addRelationshipKim
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=kim   movieId=city
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addActorwithAgeMariya
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Mariya Gorelkina    actorId=mariya    age=27
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200
    Should Be Equal As Strings    ${resp.status_code}    200

addActorwithAgeFrank
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Frank Alfonso    actorId=frank    age=27
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200
    Should Be Equal As Strings    ${resp.status_code}    200

addRelationshipmariyafake2
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=mariya   movieId=fake2
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addRelationshipfrankfake2
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=frank   movieId=fake2
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addRelationshipfrankfake1
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=frank   movieId=fake1
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addRelationshipKevinfake1
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=nm0000102   movieId=fake1
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addRelationshipMariyaCity
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=mariya   movieId=city
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

computeKevinBaconPath2EqualRoutes
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=mariya
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconPath    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"baconPath":["mariya","city","sarah","foot","nm0000102"]}

addActorwithAgePass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Merril Streep    actorId=Merril    age=69
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200
    Should Be Equal As Strings    ${resp.status_code}    200

addActorwithEmptyAgePass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Tory Branch    actorId=ToryB23    age=
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200
    Should Be Equal As Strings    ${resp.status_code}    200

addActorwithAgeFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Merril Streep    actorId=Merril    age=69
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=400
    Should Be Equal As Strings    ${resp.status_code}    400

getActorsByAgePass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    age=27
    ${resp}=    GET On Session    localhost    /api/v1/getActorsByAge    params=${params}    headers=${headers}    expected_status=200
    Should Be Equal As Strings    ${resp.status_code}    200
    ${response_body}=    Set Variable    ${resp.json()}
    ${expected_actors}=    Create List    Mariya Gorelkina    Frank Alfonso
    Should Be Equal    ${response_body["actors"]}    ${expected_actors}

getActorsByAgeFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    age=
    ${resp}=    GET On Session    localhost    /api/v1/getActorsByAge    params=${params}    headers=${headers}    expected_status=400
    Should Be Equal As Strings    ${resp.status_code}    400

getActorsByAgeFail404
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    age=28
    ${resp}=    GET On Session    localhost    /api/v1/getActorsByAge    params=${params}    headers=${headers}    expected_status=404
    Should Be Equal As Strings    ${resp.status_code}    404

getActorFail404
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tom
    ${resp}=    GET On Session    localhost    /api/v1/getActor    params=${params}    headers=${headers}    expected_status=404
    Should Be Equal As Strings    ${resp.status_code}    404

getMovieFail404
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    movieId=forrests
    ${resp}=    GET On Session    localhost    /api/v1/getMovie    params=${params}    headers=${headers}    expected_status=404
    Should Be Equal As Strings    ${resp.status_code}    404

addRelationshipFail404
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tom   movieId=forrest
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=404
    Should Be Equal As Strings    ${resp.status_code}    404

hasRelationshipFail404
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tommy   movieId=forrest
    ${resp}=    GET On Session    localhost    /api/v1/hasRelationship    params=${params}    headers=${headers}    expected_status=404
    Should Be Equal As Strings    ${resp.status_code}    404

computeBaconNumberFail404
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=invalidActorId
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconNumber    params=${params}    headers=${headers}    expected_status=404

computeBaconPathFail404
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=Merril
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconPath    params=${params}    headers=${headers}    expected_status=404
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.status_code}    404

addActorPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Tom Hanks    actorId=tom
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=200

addActorFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Tom Hanks
    ${resp}=    PUT On Session    localhost    /api/v1/addActor    json=${params}    headers=${headers}    expected_status=400

addMoviePass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Forrest Gump    movieId=forrest
    ${resp}=    PUT On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}    expected_status=200

addMovieFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Forrest Gump
    ${resp}=    PUT On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}    expected_status=400

addRelationshipPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tom   movieId=forrest
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=200

addRelationshipFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tom
    ${resp}=    PUT On Session    localhost    /api/v1/addRelationship    json=${params}    headers=${headers}    expected_status=400

getActorPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tom
    ${resp}=    GET On Session    localhost    /api/v1/getActor    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"actorId":"tom","name":"Tom Hanks","movies":["forrest"]}

getActorPass2
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=Merril
    ${resp}=    GET On Session    localhost    /api/v1/getActor    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"actorId":"Merril","name":"Merril Streep","movies":[]}

getActorFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Tom Hanks
    ${resp}=    GET On Session    localhost    /api/v1/getActor    params=${params}    headers=${headers}    expected_status=400

addMoviePassUnconnected
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=Avengers    movieId=mcu
    ${resp}=    PUT On Session    localhost    /api/v1/addMovie    json=${params}    headers=${headers}    expected_status=200    

getMoviePass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    movieId=forrest
    ${resp}=    GET On Session    localhost    /api/v1/getMovie    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"movieId":"forrest","name":"Forrest Gump","actors":["tom"]}

getMoviePass2
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    movieId=mcu
    ${resp}=    GET On Session    localhost    /api/v1/getMovie    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"movieId":"mcu","name":"Avengers","actors":[]}

getMovieFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    name=forrest
    ${resp}=    GET On Session    localhost    /api/v1/getMovie    params=${params}    headers=${headers}    expected_status=400

hasRelationshipPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tom    movieId=forrest
    ${resp}=    GET On Session    localhost    /api/v1/hasRelationship    params=${params}    headers=${headers}    expected_status=200

hasRelationshipFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=tom
    ${resp}=    GET On Session    localhost    /api/v1/hasRelationship    params=${params}    headers=${headers}    expected_status=400

computeBaconNumberPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=kim
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconNumber    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"baconNumber":2}

computeBaconNumberFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconNumber    params=${params}    headers=${headers}    expected_status=400

computeBaconPathPass
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary    actorId=kim
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconPath    params=${params}    headers=${headers}    expected_status=200
    Log    ${resp.content}
    Should Be Equal As Strings    ${resp.content}    {"baconPath":["kim","city","sarah","foot","nm0000102"]}

computeBaconPathFail
    ${headers}=    Create Dictionary    Content-Type=application/json
    ${params}=    Create Dictionary
    ${resp}=    GET On Session    localhost    /api/v1/computeBaconPath    params=${params}    headers=${headers}    expected_status=400
