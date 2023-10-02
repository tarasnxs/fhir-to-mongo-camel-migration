const LOG = org.slf4j.LoggerFactory.getLogger(exchange.getFromRouteId());

process(body);

function process(jsonList) {
  try {
    const parsedList = jsonList.map(jsonString => JSON.parse(jsonString));
    for (const jsonObj of parsedList) {
      if (jsonObj.hasOwnProperty("birthDate")) {
        jsonObj["birthDate"] = null;
      }
    }
    return JSON.stringify(parsedList);
  } catch (error) {
    LOG.error('Error modifying JSON list:', error);
  }
}