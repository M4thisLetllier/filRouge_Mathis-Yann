package ca.uqac.c8INF865.test.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Structure d'un contact
data class Contact(
    val id: Int,
    val name: String,
    val phone: String
)

// Pour les tests tant qu'on a pas de bdd
val testContacts = listOf(
    Contact(1, "Alexandre Tremblay", "+1 (418)-555-0123"),
    Contact(2, "Marie Gagnon", "+1 (581)-555-0456"),
    Contact(3, "Gabriel Côté", "+1 (418)-555-0789"),
    Contact(4, "Émilie Bouchard", "+1 (581)-555-0234"),
    Contact(5, "Thomas Girard", "+1 (367)-555-0567")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranContact() {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {}, // A RAJOUTER PLUS TARD SI ON MET QUELQUE CHOSE EN HAUT
        floatingActionButton = {
            FloatingActionButton(
                onClick = { },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter un contact"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Rechercher un contact...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Icône de recherche"
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(testContacts) { contact ->
                    ContactCard(contact = contact)
                }
            }
        }
    }
}

// Ca c'est une brique d'une personne dans la liste, utilisable à l'infini
@Composable
fun ContactCard(contact: Contact) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = contact.name,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phone,
                )
            }
        }
    }
}