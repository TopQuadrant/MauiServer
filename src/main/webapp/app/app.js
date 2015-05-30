
var taggers;

$(function() {
  $('#results').hide();
  $.getJSON('/', function(data) {
    taggers = data.taggers;
    updateTaggerList();
  }).fail(function() {
    $('#tagger option').text('Failed to retrieve tagger list!');
  });
  $('#tagger').change(function() {
    $('#submit').attr('disabled', $('#tagger').val() == true)
  });
  $('#submit').click(function(e) {
    e.preventDefault();
    submit($('#tagger').val(), $('#text').val());
  });
});

function updateTaggerList() {
  if (!taggers.length) {
    $('#tagger').html('<option selected disabled>No taggers created!</option>');
    return;
  }
  var html = '<option selected disabled>Select a tagger!</option>';
  var template = _.template('<option value="<%- id %>"><%- title %></option>');
  _.each(taggers, function(tagger) {
    html += template(tagger);
  });
  $('#tagger').attr('disabled', false).html(html);
}

function getTaggerURL(taggerId) {
  var tagger = _.find(taggers, function(tagger) {
    return taggerId == tagger.id
  });
  if (!tagger) return null;
  return '..' + tagger.href;
}

function updateResultsView(topics) {
  $('#results').show();
  if (!topics.length) {
    $('#topics').html('<em>No appropriate concepts found</em>');
    return;
  }
  var html = '<table><tr><th>Topic</th><th>Identifier</th><th>Confidence</th></tr>';
  var template = _.template('<tr><td class=label><%- label %></td><td class=topic_id><%- id %></td><td class=probability><%- probability %></td></tr>');
  _.each(topics, function(topic) {
    topic.probability = Math.round(topic.probability * 100) + '%';
    html += template(topic);
  });
  html += '</table>';
  $('#topics').html(html);
}

function submit(taggerId, text) {
  $.post(getTaggerURL(taggerId) + '/suggest', {text: text}, function(data) {
    updateResultsView(data.topics);
  }, 'json')
  .fail(function(jqXHR, textStatus, errorThrown) {
    $('#results').show();
    var message = jqXHR.responseJSON ?
        jqXHR.responseJSON.message : 'Failed to connect to server';
    $('#topics').html('<em>' + message + '</em>');
  });
}
