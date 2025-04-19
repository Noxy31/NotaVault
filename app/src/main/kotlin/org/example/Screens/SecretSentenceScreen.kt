package org.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.desktop.ui.tooling.preview.Preview

@Composable
fun SecretSentenceScreen(
    secretSentence: String,
    onContinue: () -> Unit
) {
    ModernDarkTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    )
                )
        ) {
            Card(
                modifier = Modifier
                    .width(450.dp)
                    .padding(16.dp)
                    .align(Alignment.Center),
                shape = RoundedCornerShape(16.dp),
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Icône
                    Surface(
                        modifier = Modifier.size(70.dp),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colors.primary.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "🔐",
                                fontSize = MaterialTheme.typography.h3.fontSize,
                                color = MaterialTheme.colors.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Titre
                    Text(
                        text = "Votre phrase secrète",
                        style = MaterialTheme.typography.h4,
                        color = Color.White
                    )
                    
                    // Message d'information
                    Text(
                        text = "Cette phrase vous permettra de récupérer votre compte en cas d'oubli de mot de passe. " +
                               "Notez-la précieusement dans un endroit sûr.",
                        style = MaterialTheme.typography.subtitle1,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Affichage de la phrase secrète
                    Surface(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                       shape = RoundedCornerShape(8.dp),
                          color = MaterialTheme.colors.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                           text = secretSentence,
                           style = MaterialTheme.typography.h6,
                           color = Color.White,
                           textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                     )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Message d'avertissement
                    Text(
                        text = "ATTENTION: Cette phrase ne sera affichée qu'une seule fois!",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.error,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Bouton de continuation
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    ) {
                        Text(
                            text = "J'AI NOTÉ MA PHRASE",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewSecretSentenceScreen() {
    SecretSentenceScreen(
        secretSentence = "Le grand chat danse aujourd'hui.",
        onContinue = {}
    )
}