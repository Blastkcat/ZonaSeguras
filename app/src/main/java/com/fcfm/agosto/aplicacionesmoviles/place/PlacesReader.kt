// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.fcfm.agosto.aplicacionesmoviles.place

import android.content.Context
import com.fcfm.agosto.aplicacionesmoviles.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class PlacesReader(private val context: Context) {

    private val db = Firebase.firestore

    suspend fun read(): List<Place> {
        val results = db.collection(R.string.placesFirestore.toString()).get().await();
        val placesResponse = results.documents.mapNotNull { it.toObject(PlaceResponse::class.java) }
        return placesResponse.map { it.toPlace() }
    }
}