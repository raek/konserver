/* The content of this file is in the public domain. */

var users;
var users_by_uri;
var members;
var members_by_user_uri;

function link_to_member(uri) {
  var member = members_by_user_uri[uri];
  if (member) {
    return '<a href="' + member.uri + '">' + member.name + '</a>';
  } else {
    var user = users_by_uri[uri];
    return '<i><a href="' + uri + '">' + user.name + '</a></i>';
  }
}

function with_sign(x) {
  if (x > 0) {
    return "+" + x;
  } else if (x < 0) {
    return "−" + (-x);
  } else {
    return "±0";
  }
}

function makeRow(debt) {
  return $('<tr>' +
    '<td><a href="' + debt.uri + '">' + debt.name + '</a></td>' +
    '<td class="num">' + debt.amount + '</td>' +
    '<td>' + link_to_member(debt.creditor) + ' <span class="change">(' + with_sign(debt["to-creditor"]) + ')</span></td>' +
    '<td>' + debt.debtors.map(link_to_member).join(', ') + ' <span class="change">(' + with_sign(-debt["from-debtor"]) + ')</span></td>' +
  '</tr>');
}

function makeOption(label, value) {
  return $('<option value="' + value + '">' + label + '</option>');
}

function makeCheckbox(label, id, value) {
  return $('<div><input type="checkbox" id="' + id + '" name="' + id + '" value="' + value + '" /><label for="' + id + '">' + label + '</label></div>');
}

function fail(xhr) {
  $("#loading").hide();
  alert("HTTP error: " + xhr.status);
}

function onReady() {
  konserver.get(user_coll_uri, function(user_coll) {
    users = user_coll.users;
    users_by_uri = new Object();
    for (var i = 0; i < users.length; i++) {
      users_by_uri[users[i].uri] = users[i];
    }
    
    konserver.get(member_coll_uri, function(member_coll) {
      members = member_coll.members;
      members_by_user_uri = new Object();
      for (var i = 0; i < members.length; i++) {
        members_by_user_uri[members[i].user] = members[i];
        makeOption(members[i].name, members[i].user).appendTo("#creditor");
        makeCheckbox(members[i].name, 'debtor' + i, members[i].user).appendTo("#debtors");
      }
      var debts = debt_coll.debts;
      for (var i = 0; i < debts.length; i++) {
        makeRow(debts[i]).appendTo("#debts");
      }
      
      $("#loading").hide();
    }, fail);
  }, fail);
}

